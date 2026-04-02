package uk.gov.hmcts.cp.servicebus.integration;

import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClient;
import com.azure.messaging.servicebus.administration.models.QueueProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class ServiceBusTestService {

    @Autowired
    ServiceBusAdministrationClient administrationClient;

    public void dropQueueIfExists(String queueName) {
        List<String> queues = administrationClient.listQueues().stream().map(QueueProperties::getName).toList();
        if (queues.contains(queueName)) {
            administrationClient.deleteQueue(queueName);
        }
    }
}