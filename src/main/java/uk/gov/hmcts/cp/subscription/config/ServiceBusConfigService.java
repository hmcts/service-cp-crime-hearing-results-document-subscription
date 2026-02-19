package uk.gov.hmcts.cp.subscription.config;

import com.azure.core.http.HttpClient;
import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClient;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClientBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;
import java.net.URL;

@Slf4j
@Service
@Getter
public class ServiceBusConfigService {
    public static final int ADMIN_CONNECTION_PORT = 5300;
    public static final String TOPIC_NAME = "amp-topics";

    private final boolean enabled;
    private final String adminConnectionString;
    private final String connectionString;
    private final int maxTries;

    public ServiceBusConfigService(
            @Value("${service-bus.enabled}") final boolean enabled,
            @Value("${service-bus.admin-connection}") final String adminConnectionString,
            @Value("${service-bus.connection}") final String connectionString,
            @Value("${service-bus.max-tries}") final int maxTries
    ) {
        log.info("ServiceBusConfigService initialised with enabled {}", enabled);
        log.info("ServiceBusConfigService initialised with adminConnectionString \"{}\"", adminConnectionString);
        log.info("ServiceBusConfigService initialised with connectionString \"{}\"", connectionString);
        log.info("ServiceBusConfigService initialised with maxTries \"{}\"", maxTries);
        this.enabled = enabled;
        this.adminConnectionString = adminConnectionString;
        this.connectionString = connectionString;
        this.maxTries = maxTries;
    }

    public ServiceBusSenderClient senderClient(final String topicName) {
        return new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .sender()
                .topicName(topicName)
                .buildClient();
    }

    public ServiceBusClientBuilder.ServiceBusProcessorClientBuilder processorClientBuilder(final String topicName, final String subscriptionName) {
        return new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .processor()
                .topicName(topicName)
                .subscriptionName(subscriptionName);
    }

    public ServiceBusAdministrationClient adminClient() {
        final HttpClient adminHttpClient = new NettyAsyncHttpClientBuilder()
                .port(ADMIN_CONNECTION_PORT)
                .build();
        final HttpPipelinePolicy forceHttpPolicy = (context, next) -> {
            try {
                final URL current = context.getHttpRequest().getUrl();
                final URL httpUrl = new URL("http", current.getHost(), ADMIN_CONNECTION_PORT, current.getFile());
                context.getHttpRequest().setUrl(httpUrl);
            } catch (MalformedURLException e) {
                return Mono.error(e);
            }
            return next.process();
        };

        return new ServiceBusAdministrationClientBuilder()
                .connectionString(adminConnectionString)
                .httpClient(adminHttpClient)
                .addPolicy(forceHttpPolicy)
                .buildClient();
    }
}
