package uk.gov.hmcts.cp.subscription.config;

import com.azure.core.http.HttpClient;
import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
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
    private boolean enabled;
    private String adminConnectionString;
    private String connectionString;
    private int maxTries;

    public ServiceBusConfigService(
            @Value("${service-bus.enabled}") boolean enabled,
            @Value("${service-bus.admin-connection}") String adminConnectionString,
            @Value("${service-bus.connection}") String connectionString,
            @Value("${service-bus.max-tries}") int maxTries
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

    public ServiceBusClientBuilder clientBuilder() {
        return new ServiceBusClientBuilder().connectionString(connectionString);
    }

    public ServiceBusAdministrationClient adminClient() {
        HttpClient adminHttpClient = new NettyAsyncHttpClientBuilder()
                .port(ADMIN_CONNECTION_PORT)
                .build();
        HttpPipelinePolicy forceHttpPolicy = (context, next) -> {
            try {
                URL current = context.getHttpRequest().getUrl();
                URL httpUrl = new URL("http", current.getHost(), ADMIN_CONNECTION_PORT, current.getFile());
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

    public ServiceBusClientBuilder.ServiceBusProcessorClientBuilder processorClientBuilder(String topicName, String subscriptionName) {
        return clientBuilder()
                .processor()
                .topicName(topicName)
                .subscriptionName(subscriptionName);
    }
}
