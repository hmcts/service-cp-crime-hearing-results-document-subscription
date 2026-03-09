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
import uk.gov.hmcts.cp.servicebus.mapper.ServiceBusWrapperMapper;
import uk.gov.hmcts.cp.servicebus.model.ServiceBusWrappedMessage;
import uk.gov.hmcts.cp.subscription.services.JsonMapper;

import java.time.OffsetDateTime;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceBusClientServiceTest {

    @Mock
    ServiceBusConfigService configService;
    @Mock
    ServiceBusWrapperMapper wrapperMapper;
    @Mock
    ServiceBusRetryService retryService;
    @Mock
    ServiceBusMapper mapper;
    @Mock
    JsonMapper jsonMapper;

    @InjectMocks
    ServiceBusClientService clientService;

    @Mock
    ServiceBusSenderClient senderClient;

    OffsetDateTime nextTryTime = OffsetDateTime.now();
    String callbackUrl = "http://callback";
    ServiceBusWrappedMessage wrappedMessage = ServiceBusWrappedMessage.builder().build();
    ServiceBusMessage serviceBusMessage = new ServiceBusMessage("wrapped-Message");

    @Test
    void queue_message_should_pass_to_topic() {
        when(configService.senderClient("topic1")).thenReturn(senderClient);
        when(wrapperMapper.newWrapper(1, callbackUrl, "message")).thenReturn(wrappedMessage);
        when(jsonMapper.toJson(wrappedMessage)).thenReturn("wrapped-message");
        when(retryService.getNextTryTime(1)).thenReturn(nextTryTime);
        when(mapper.newMessage("wrapped-message", nextTryTime)).thenReturn(serviceBusMessage);

        clientService.queueMessage("topic1", callbackUrl, "message", 1);

        verify(senderClient).sendMessage(serviceBusMessage);
        verify(senderClient).close();
    }
}