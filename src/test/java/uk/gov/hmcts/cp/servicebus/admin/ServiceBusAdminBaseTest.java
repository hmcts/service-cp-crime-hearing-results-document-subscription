package uk.gov.hmcts.cp.servicebus.admin;

import com.azure.core.http.rest.PagedIterable;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClient;
import com.azure.messaging.servicebus.administration.models.CreateQueueOptions;
import com.azure.messaging.servicebus.administration.models.QueueProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceBusAdminBaseTest {

    @Mock
    private ServiceBusAdministrationClient adminClient;
    @Mock
    private PagedIterable<QueueProperties> queuePage;
    @Captor
    private ArgumentCaptor<CreateQueueOptions> optionsCaptor;

    private ServiceBusAdminBase subject;

    @BeforeEach
    void setUp() {
        subject = new ServiceBusAdminBase(adminClient) {};
    }

    @Test
    void listQueues_delegatesToAdminClient() {
        when(adminClient.listQueues()).thenReturn(queuePage);
        assertThat(subject.listQueues()).isSameAs(queuePage);
    }

    @Test
    void getQueueExists_delegatesToAdminClient() {
        when(adminClient.getQueueExists("q1")).thenReturn(true);
        assertThat(subject.getQueueExists("q1")).isTrue();
    }

    @Test
    void createQueue_delegatesToAdminClientWithOptions() {
        subject.createQueue("q1");
        verify(adminClient).createQueue(eq("q1"), optionsCaptor.capture());
        assertThat(optionsCaptor.getValue().getDefaultMessageTimeToLive()).isEqualTo(Duration.ofHours(1));
        assertThat(optionsCaptor.getValue().getMaxDeliveryCount()).isEqualTo(1);
    }

    @Test
    void getAdminClient_returnsUnderlyingClient() {
        assertThat(subject.getAdminClient()).isSameAs(adminClient);
    }
}