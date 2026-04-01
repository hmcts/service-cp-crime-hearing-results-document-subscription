package uk.gov.hmcts.cp.servicebus.integration;

import com.azure.messaging.servicebus.administration.models.QueueProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.servicebus.admin.ServiceBusAdminInterface;

import java.util.List;

@Slf4j
@Service
public class ServiceBusTestService {

    @Autowired
    ServiceBusAdminInterface adminClient;

    public void dropQueueIfExists(String queueName) {
        List<String> queues = adminClient.listQueues().stream().map(QueueProperties::getName).toList();
        if (queues.contains(queueName)) {
            adminClient.deleteQueue(queueName);
        }
    }
}