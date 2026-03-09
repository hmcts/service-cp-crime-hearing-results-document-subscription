package uk.gov.hmcts.cp.servicebus.services;

import com.azure.core.util.BinaryData;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;
import uk.gov.hmcts.cp.openapi.model.EventNotificationPayload;
import uk.gov.hmcts.cp.servicebus.config.ServiceBusConfigService;
import uk.gov.hmcts.cp.servicebus.mapper.ServiceBusMapper;
import uk.gov.hmcts.cp.servicebus.model.ServiceBusWrappedMessage;
import uk.gov.hmcts.cp.subscription.clients.CallbackClient;
import uk.gov.hmcts.cp.subscription.mappers.NotificationMapper;
import uk.gov.hmcts.cp.subscription.services.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.cp.servicebus.config.ServiceBusConfigService.PCR_OUTBOUND_TOPIC;

@ExtendWith(MockitoExtension.class)
class ServiceBusProcessorServiceTest {
    @Mock
    JsonMapper jsonMapper = new JsonMapper();
    @Mock
    ServiceBusMapper serviceBusMapper;
    @Mock
    ServiceBusConfigService configService;
    @Mock
    NotificationMapper notificationMapper;
    @Mock
    CallbackClient callbackClient;
    @Mock
    ServiceBusClientService serviceBusClientService;

    @InjectMocks
    ServiceBusProcessorService serviceBusProcessorService;

    @Mock
    ServiceBusProcessorClient processorClient;
    @Mock
    ServiceBusClientBuilder.ServiceBusProcessorClientBuilder processorClientBuilder;

    @Mock
    BinaryData binaryData;
    @Mock
    ServiceBusReceivedMessage serviceBusReceivedMessage;

    @Mock
    ServiceBusReceivedMessageContext context;
    @Mock
    ServiceBusWrappedMessage wrappedMessage;

    private String callbackUrl = "http://callback";

    @Test
    void handle_message_should_pass_to_callback_client_with_success() {
        EventNotificationPayload notificationPayload = EventNotificationPayload.builder().build();
        when(context.getMessage()).thenReturn(serviceBusReceivedMessage);
        when(serviceBusReceivedMessage.getBody()).thenReturn(binaryData);
        when(jsonMapper.fromJson("binaryData", ServiceBusWrappedMessage.class)).thenReturn(wrappedMessage);
        when(wrappedMessage.getMessage()).thenReturn("message");
        when(jsonMapper.fromJson("message", EventNotificationPayload.class)).thenReturn(notificationPayload);
        when(wrappedMessage.getTargetUrl()).thenReturn(callbackUrl);

        serviceBusProcessorService.handleMessage(PCR_OUTBOUND_TOPIC, context);

        verify(callbackClient).sendNotification(callbackUrl, notificationPayload);
    }

    @Test
    void handle_message_should_requeue_if_callback_client_errors() {
        EventNotificationPayload notificationPayload = EventNotificationPayload.builder().build();
        when(context.getMessage()).thenReturn(serviceBusReceivedMessage);
        when(serviceBusReceivedMessage.getBody()).thenReturn(binaryData);
        when(jsonMapper.fromJson("binaryData", ServiceBusWrappedMessage.class)).thenReturn(wrappedMessage);
        when(wrappedMessage.getMessage()).thenReturn("wrapped-message");
        when(jsonMapper.fromJson("wrapped-message", EventNotificationPayload.class)).thenReturn(notificationPayload);
        when(wrappedMessage.getTargetUrl()).thenReturn(callbackUrl);
        doThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR)).when(callbackClient)
                .sendNotification(callbackUrl, notificationPayload);

        when(configService.getMaxTries()).thenReturn(2);

        serviceBusProcessorService.handleMessage(PCR_OUTBOUND_TOPIC, context);

        verify(serviceBusClientService).queueMessage(PCR_OUTBOUND_TOPIC, callbackUrl, "wrapped-message", 1);
    }

    @Test
    void handle_message_should_error_if_callback_client_errors_finally() {
        EventNotificationPayload notificationPayload = EventNotificationPayload.builder().build();
        when(context.getMessage()).thenReturn(serviceBusReceivedMessage);
        when(serviceBusReceivedMessage.getBody()).thenReturn(binaryData);
        when(jsonMapper.fromJson("binaryData", ServiceBusWrappedMessage.class)).thenReturn(wrappedMessage);
        when(wrappedMessage.getMessage()).thenReturn("wrapped-message");
        when(jsonMapper.fromJson("wrapped-message", EventNotificationPayload.class)).thenReturn(notificationPayload);
        when(wrappedMessage.getTargetUrl()).thenReturn(callbackUrl);
        doThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR)).when(callbackClient)
                .sendNotification(callbackUrl, notificationPayload);
        when(configService.getMaxTries()).thenReturn(1);

        assertThrows(HttpServerErrorException.class, () -> serviceBusProcessorService.handleMessage(PCR_OUTBOUND_TOPIC, context));
    }
}