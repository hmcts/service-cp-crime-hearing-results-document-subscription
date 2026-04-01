package uk.gov.hmcts.cp.servicebus.admin;

import com.azure.core.http.rest.PagedIterable;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClient;
import com.azure.messaging.servicebus.administration.models.CreateQueueOptions;
import com.azure.messaging.servicebus.administration.models.QueueProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceBusAdminClientTest {

    @Mock
    private ServiceBusAdministrationClient adminClient;
    @Mock
    private PagedIterable<QueueProperties> queuePage;

    private ServiceBusAdminClient subject;

    @BeforeEach
    void setUp() {
        subject = new ServiceBusAdminClient(adminClient);
    }

    @Test
    void listQueues_delegatesToAdminClient() {
        when(adminClient.listQueues()).thenReturn(queuePage);

        assertThat(subject.listQueues()).isSameAs(queuePage);
        verify(adminClient).listQueues();
    }

    @Test
    void getQueueExists_delegatesToAdminClient() {
        when(adminClient.getQueueExists("q1")).thenReturn(true);

        assertThat(subject.getQueueExists("q1")).isTrue();
        verify(adminClient).getQueueExists("q1");
    }

    @Test
    void createQueue_delegatesToAdminClient() {
        final CreateQueueOptions options = new CreateQueueOptions();

        subject.createQueue("q1", options);

        verify(adminClient).createQueue("q1", options);
    }

    @Test
    void deleteQueue_delegatesToAdminClient() {
        subject.deleteQueue("q1");

        verify(adminClient).deleteQueue("q1");
    }
}
