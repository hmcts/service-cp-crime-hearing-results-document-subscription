package uk.gov.hmcts.cp.servicebus.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cp.openapi.model.EventNotificationPayload;
import uk.gov.hmcts.cp.openapi.model.PcrEventPayload;
import uk.gov.hmcts.cp.subscription.clients.CallbackClient;
import uk.gov.hmcts.cp.subscription.managers.NotificationManager;
import uk.gov.hmcts.cp.subscription.services.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.cp.servicebus.config.ServiceBusConfigService.PCR_INBOUND_TOPIC;
import static uk.gov.hmcts.cp.servicebus.config.ServiceBusConfigService.PCR_OUTBOUND_TOPIC;

@ExtendWith(MockitoExtension.class)
class ServiceBusHandlersTest {
    @Mock
    JsonMapper jsonMapper;
    @Mock
    NotificationManager notificationManager;
    @Mock
    CallbackClient callbackClient;

    @InjectMocks
    ServiceBusHandlers serviceBusHandlers;

    @Test
    void inbound_pcr_should_handle_ok() {
        PcrEventPayload pcrEventPayload = PcrEventPayload.builder().build();
        when(jsonMapper.fromJson("pcr-json", PcrEventPayload.class)).thenReturn(pcrEventPayload);

        serviceBusHandlers.handleMessage(PCR_INBOUND_TOPIC, null, "pcr-json");

        verify(notificationManager).processPcrNotification(pcrEventPayload);
    }

    @Test
    void outbound_callback_should_handle_ok() {
        EventNotificationPayload notificationPayload = EventNotificationPayload.builder().build();
        when(jsonMapper.fromJson("callback-json", EventNotificationPayload.class)).thenReturn(notificationPayload);

        serviceBusHandlers.handleMessage(PCR_OUTBOUND_TOPIC, "https://callback", "callback-json");

        verify(callbackClient).sendNotification("https://callback", notificationPayload);
    }

    @Test
    void unknown_topic_should_throw_error(){
        assertThrows(RuntimeException.class, ()-> serviceBusHandlers.handleMessage("BAD-TOPIC", null, "callback-json"));
    }
}