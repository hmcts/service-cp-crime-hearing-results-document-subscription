package uk.gov.hmcts.cp.servicebus.admin;

import com.azure.core.http.HttpClient;
import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClientBuilder;
import reactor.core.publisher.Mono;
import uk.gov.hmcts.cp.servicebus.config.ServiceBusProperties;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import static uk.gov.hmcts.cp.servicebus.config.ServiceBusProperties.ADMIN_CONNECTION_PORT;

public class ServiceBusAdminEmulatorImpl extends ServiceBusAdminBase {

    public ServiceBusAdminEmulatorImpl(final ServiceBusProperties properties) {
        super(new ServiceBusAdministrationClientBuilder()
                .connectionString(properties.getAdminConnectionString())
                .httpClient(buildHttpClient())
                .addPolicy(buildForceHttpPolicy())
                .buildClient());
    }

    private static HttpClient buildHttpClient() {
        return new NettyAsyncHttpClientBuilder().port(ADMIN_CONNECTION_PORT).build();
    }

    private static HttpPipelinePolicy buildForceHttpPolicy() {
        return (context, next) -> {
            try {
                final URL current = context.getHttpRequest().getUrl();
                final URL httpUrl = URI.create("http://" + current.getHost() + ":" + ADMIN_CONNECTION_PORT + current.getFile()).toURL();
                context.getHttpRequest().setUrl(httpUrl);
            } catch (MalformedURLException e) {
                return Mono.error(e);
            }
            return next.process();
        };
    }
}