package uk.gov.hmcts.cp.servicebus.services;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.cp.servicebus.config.ServiceBusProperties;

@Component
@AllArgsConstructor
public class ServiceBusClientFactory {

    private final ServiceBusProperties properties;

    public ServiceBusSenderClient senderClient(final String queueName) {
        return clientBuilder().sender().queueName(queueName).buildClient();
    }

    public ServiceBusClientBuilder.ServiceBusProcessorClientBuilder processorClientBuilder(final String queueName) {
        return clientBuilder().processor().queueName(queueName);
    }

    private ServiceBusClientBuilder clientBuilder() {
        return new ServiceBusClientBuilder().connectionString(properties.getConnectionString());
    }
}