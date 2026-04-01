package uk.gov.hmcts.cp.servicebus.admin;

import com.azure.core.http.rest.PagedIterable;
import com.azure.messaging.servicebus.administration.models.CreateQueueOptions;
import com.azure.messaging.servicebus.administration.models.QueueProperties;

public interface ServiceBusAdminInterface {

    PagedIterable<QueueProperties> listQueues();

    boolean getQueueExists(String queueName);

    void createQueue(String queueName, CreateQueueOptions options);

    void deleteQueue(String queueName);
}