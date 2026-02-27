package uk.gov.hmcts.cp.servicebus.services;

import com.azure.core.util.BinaryData;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;
import uk.gov.hmcts.cp.openapi.model.EventNotificationPayload;
import uk.gov.hmcts.cp.servicebus.mapper.ServiceBusMapper;
import uk.gov.hmcts.cp.servicebus.model.ServiceBusMessageWrapper;
import uk.gov.hmcts.cp.subscription.clients.CallbackClient;
import uk.gov.hmcts.cp.subscription.config.ServiceBusConfigService;
import uk.gov.hmcts.cp.subscription.mappers.NotificationMapper;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import org.slf4j.LoggerFactory;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceBusProcessorServiceTest {
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

    UUID correlationId = UUID.fromString("b159b5fe-f602-400f-8bff-f06118e2f5bb");

    @Mock
    BinaryData binaryData;
    @Mock
    ServiceBusReceivedMessage serviceBusReceivedMessage;


    @Mock
    ServiceBusReceivedMessageContext context;

    @Test
    @Disabled
    void start_message_processor_should_start() {
        // TODO FIXUP ?? Struggling to mock Consumer<ServiceBusReceivedMessageContext>
        // .processorClientBuilder(topicName, subscriptionName) ... OK
        when(configService.processorClientBuilder("topic1", "subscription1")).thenReturn(processorClientBuilder);
        // .processMessage(context -> handleMessage(topicName, subscriptionName, context)) ... HOW MOCK THIS ??
        // .processError(context -> handleError(topicName, subscriptionName)) ... AND THIS

        ServiceBusProcessorClient returned = serviceBusProcessorService.startMessageProcessor("topic1", "subscription1");

        verify(processorClient).start();
        assertThat(returned).isEqualTo(processorClient);
    }

    @Test
    void handle_message_should_pass_to_callback_client_with_success() {
        ServiceBusMessageWrapper wrapper = ServiceBusMessageWrapper.builder().correlationId(correlationId).message("wrapped-message").build();
        EventNotificationPayload notificationPayload = EventNotificationPayload.builder().build();
        when(context.getMessage()).thenReturn(serviceBusReceivedMessage);
        when(serviceBusReceivedMessage.getBody()).thenReturn(binaryData);
        when(serviceBusMapper.mapFromJson(anyString())).thenReturn(wrapper);
        when(notificationMapper.mapFromJson("wrapped-message")).thenReturn(notificationPayload);

        serviceBusProcessorService.handleMessage("topic1", "subscription1", context);

        verify(callbackClient).sendNotification("todo-url-for-sub", notificationPayload);
    }

    @Test
    void handle_message_should_requeue_if_callback_client_errors() {
        ServiceBusMessageWrapper wrapper = ServiceBusMessageWrapper.builder().correlationId(correlationId).message("wrapped-message").build();
        EventNotificationPayload notificationPayload = EventNotificationPayload.builder().build();
        when(context.getMessage()).thenReturn(serviceBusReceivedMessage);
        when(serviceBusReceivedMessage.getBody()).thenReturn(binaryData);
        when(serviceBusMapper.mapFromJson(anyString())).thenReturn(wrapper);
        when(notificationMapper.mapFromJson("wrapped-message")).thenReturn(notificationPayload);
        doThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR)).when(callbackClient)
                .sendNotification("todo-url-for-sub", notificationPayload);

        when(configService.getMaxTries()).thenReturn(2);

        serviceBusProcessorService.handleMessage("topic1", "subscription1", context);

        verify(serviceBusClientService).queueMessage("topic1", correlationId, "wrapped-message", 1);
    }

    @Test
    void handle_message_should_error_if_callback_client_errors_finally() {
        ServiceBusMessageWrapper wrapper = ServiceBusMessageWrapper.builder().correlationId(correlationId).message("wrapped-message").build();
        EventNotificationPayload notificationPayload = EventNotificationPayload.builder().build();
        when(context.getMessage()).thenReturn(serviceBusReceivedMessage);
        when(serviceBusReceivedMessage.getBody()).thenReturn(binaryData);
        when(serviceBusMapper.mapFromJson(anyString())).thenReturn(wrapper);
        when(notificationMapper.mapFromJson("wrapped-message")).thenReturn(notificationPayload);
        doThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR)).when(callbackClient)
                .sendNotification("todo-url-for-sub", notificationPayload);

        when(configService.getMaxTries()).thenReturn(1);

        assertThrows(HttpServerErrorException.class, () -> serviceBusProcessorService.handleMessage("topic1", "subscription1", context));
    }
}