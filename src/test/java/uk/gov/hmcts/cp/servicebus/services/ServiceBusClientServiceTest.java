package uk.gov.hmcts.cp.servicebus.services;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cp.servicebus.config.ServiceBusConfigService;
import uk.gov.hmcts.cp.servicebus.mapper.ServiceBusMapper;
import uk.gov.hmcts.cp.subscription.services.JsonMapper;

import java.time.OffsetDateTime;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceBusClientServiceTest {

    @Mock
    JsonMapper jsonMapper;
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

    OffsetDateTime nextTryTime = OffsetDateTime.now();
    String callbackUrl = "http://callback";
    ServiceBusMessage serviceBusMessage = new ServiceBusMessage("wrappedMessage");

    @Test
    void queue_message_should_pass_to_topic() {
//        when(configService.senderClient("topic1")).thenReturn(senderClient);
//        when(retryService.getNextTryTime(0)).thenReturn(nextTryTime);
//        when(serviceBusMapper.mapToMessage("wrappedMessage", nextTryTime)).thenReturn(serviceBusMessage);
//
//        clientService.queueMessage("topic1", callbackUrl, "wrappedMessage", 0);
//
//        verify(senderClient).sendMessage(serviceBusMessage);
//        verify(senderClient).close();
    }
}