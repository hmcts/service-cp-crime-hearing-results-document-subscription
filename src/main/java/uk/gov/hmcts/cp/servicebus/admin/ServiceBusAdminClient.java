package uk.gov.hmcts.cp.servicebus.admin;

import com.azure.core.http.rest.PagedIterable;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClient;
import com.azure.messaging.servicebus.administration.models.CreateQueueOptions;
import com.azure.messaging.servicebus.administration.models.QueueProperties;

public class ServiceBusAdminClient implements ServiceBusAdminInterface {

    private final ServiceBusAdministrationClient adminClient;

    public ServiceBusAdminClient(final ServiceBusAdministrationClient adminClient) {
        this.adminClient = adminClient;
    }

    @Override
    public PagedIterable<QueueProperties> listQueues() {
        return adminClient.listQueues();
    }

    @Override
    public boolean getQueueExists(final String queueName) {
        return adminClient.getQueueExists(queueName);
    }

    @Override
    public void createQueue(final String queueName, final CreateQueueOptions options) {
        adminClient.createQueue(queueName, options);
    }

    @Override
    public void deleteQueue(final String queueName) {
        adminClient.deleteQueue(queueName);
    }
}