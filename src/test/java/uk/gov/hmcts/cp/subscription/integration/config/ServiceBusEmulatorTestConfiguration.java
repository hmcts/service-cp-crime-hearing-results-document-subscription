package uk.gov.hmcts.cp.subscription.integration.config;

import com.azure.core.http.HttpClient;
import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClient;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClientBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;
import uk.gov.hmcts.cp.servicebus.admin.ServiceBusAdminClient;
import uk.gov.hmcts.cp.servicebus.admin.ServiceBusAdminInterface;
import uk.gov.hmcts.cp.servicebus.config.ServiceBusProperties;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import static uk.gov.hmcts.cp.servicebus.config.ServiceBusProperties.ADMIN_CONNECTION_PORT;

/**
 * Provides an emulator-backed ServiceBusAdminInterface for E2E integration tests
 * that run with service-bus.enabled=true (async queue path) but against the local emulator.
 */
@TestConfiguration
public class ServiceBusEmulatorTestConfiguration {

    @Bean
    @Primary
    public ServiceBusAdminInterface serviceBusAdminClient(final ServiceBusProperties properties) {
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

        final ServiceBusAdministrationClient adminClient = new ServiceBusAdministrationClientBuilder()
                .connectionString(properties.getAdminConnectionString())
                .httpClient(httpClient)
                .addPolicy(forceHttpPolicy)
                .buildClient();

        return new ServiceBusAdminClient(adminClient);
    }
}