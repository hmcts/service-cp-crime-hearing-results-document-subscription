package uk.gov.hmcts.cp.servicebus.services;

import com.azure.core.http.rest.PagedIterable;
import com.azure.messaging.servicebus.administration.models.QueueProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cp.servicebus.admin.ServiceBusAdminInterface;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceBusAdminServiceTest {

    @Mock
    ServiceBusAdminInterface adminClient;

    @InjectMocks
    ServiceBusAdminService adminService;

    @Mock
    PagedIterable<QueueProperties> queues;

    @Test
    void service_bus_started_should_return_true() {
        when(adminClient.listQueues()).thenReturn(queues);
        assertThat(adminService.isServiceBusReady()).isTrue();
    }

    @Test
    void service_bus_down_should_return_false() {
        when(adminClient.listQueues()).thenThrow(new RuntimeException("Connection refused"));
        assertThat(adminService.isServiceBusReady()).isFalse();
    }

    @Test
    void create_should_create_queue() {
        when(adminClient.getQueueExists("queue1")).thenReturn(false);
        adminService.createQueue("queue1");
        verify(adminClient).createQueue("queue1");
    }

    @Test
    void create_should_skip_if_queue_already_exists() {
        when(adminClient.getQueueExists("queue1")).thenReturn(true);
        adminService.createQueue("queue1");
        verify(adminClient, never()).createQueue("queue1");
    }
}