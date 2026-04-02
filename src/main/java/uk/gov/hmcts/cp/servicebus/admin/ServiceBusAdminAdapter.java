package uk.gov.hmcts.cp.servicebus.admin;

import com.azure.core.http.rest.PagedIterable;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClient;
import com.azure.messaging.servicebus.administration.models.CreateQueueOptions;
import com.azure.messaging.servicebus.administration.models.QueueProperties;

import java.time.Duration;

public class ServiceBusAdminAdapter implements ServiceBusAdminInterface {

    private final ServiceBusAdministrationClient adminClient;

    public ServiceBusAdminAdapter(final ServiceBusAdministrationClient adminClient) {
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
    public void createQueue(final String queueName) {
        final CreateQueueOptions options = new CreateQueueOptions();
        options.setDefaultMessageTimeToLive(Duration.ofHours(1));
        options.setMaxDeliveryCount(1);
        adminClient.createQueue(queueName, options);
    }

}