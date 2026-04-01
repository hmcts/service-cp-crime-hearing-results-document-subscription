package uk.gov.hmcts.cp.servicebus.config;

import com.azure.core.http.HttpClient;
import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClient;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClientBuilder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;
import uk.gov.hmcts.cp.servicebus.admin.ServiceBusAdminClient;
import uk.gov.hmcts.cp.servicebus.admin.ServiceBusAdminInterface;
import uk.gov.hmcts.cp.vault.VaultServiceProperties;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import static uk.gov.hmcts.cp.servicebus.config.ServiceBusProperties.ADMIN_CONNECTION_PORT;

@Slf4j
@Configuration
@AllArgsConstructor
public class ServiceBusAdminConfiguration {

    private ServiceBusProperties configService;
    private VaultServiceProperties vaultServiceProperties;

    @Bean
    public ServiceBusAdminInterface serviceBusAdminClient() {
        log.info("ServiceBusAdminConfiguration building serviceBusAdminClient enabled:{}", configService.isEnabled());
        if (configService.isEnabled()) {
            return new ServiceBusAdminClient(buildAzureAdminClient());
        } else {
            return new ServiceBusAdminClient(buildEmulatorAdminClient());
        }
    }

    private ServiceBusAdministrationClient buildAzureAdminClient() {
        return new ServiceBusAdministrationClientBuilder()
                .endpoint(configService.getConnectionString())
                .credential(new DefaultAzureCredentialBuilder()
                        .managedIdentityClientId(vaultServiceProperties.getVaultClientId().toString())
                        .build())
                .buildClient();
    }

    private ServiceBusAdministrationClient buildEmulatorAdminClient() {
        final HttpClient httpClient = new NettyAsyncHttpClientBuilder()
                .port(ADMIN_CONNECTION_PORT)
                .build();
        final HttpPipelinePolicy forceHttpPolicy = (context, next) -> {
            try {
                final URL current = context.getHttpRequest().getUrl();
                final URL httpUrl = URI.create("http://" + current.getHost() + ":" + ADMIN_CONNECTION_PORT + current.getFile()).toURL();
                context.getHttpRequest().setUrl(httpUrl);
            } catch (MalformedURLException e) {
                return Mono.error(e);
            }
            return next.process();
        };

        return new ServiceBusAdministrationClientBuilder()
                .connectionString(configService.getAdminConnectionString())
                .httpClient(httpClient)
                .addPolicy(forceHttpPolicy)
                .buildClient();
    }
}