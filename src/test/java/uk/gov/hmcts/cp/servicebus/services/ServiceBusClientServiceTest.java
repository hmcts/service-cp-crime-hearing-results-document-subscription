package uk.gov.hmcts.cp.servicebus.services;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cp.servicebus.mapper.ServiceBusMapper;
import uk.gov.hmcts.cp.servicebus.config.ServiceBusConfigService;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceBusClientServiceTest {

    @Mock
    ServiceBusMapper serviceBusMapper;
    @Mock
    ServiceBusConfigService configService;
    @Mock
    ServiceBusRetryService retryService;

    @InjectMocks
    ServiceBusClientService clientService;

    @Mock
    ServiceBusSenderClient senderClient;

    UUID correlationId = UUID.fromString("b159b5fe-f602-400f-8bff-f06118e2f5bb");
    OffsetDateTime nextTryTime = OffsetDateTime.now();
    ServiceBusMessage serviceBusMessage = new ServiceBusMessage("wrapped-message");

    @Test
    void queue_message_should_pass_to_topic() {
        when(configService.senderClient("topic1")).thenReturn(senderClient);
        when(serviceBusMapper.mapToJson("message", 0)).thenReturn("wrapped-message");
        when(retryService.getNextTryTime(0)).thenReturn(nextTryTime);
        when(serviceBusMapper.mapToMessage("wrapped-message", nextTryTime)).thenReturn(serviceBusMessage);

        clientService.queueMessage("topic1", "message", 0);

        verify(senderClient).sendMessage(serviceBusMessage);
        verify(senderClient).close();
    }
}