package uk.gov.hmcts.cp.subscription.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cp.hmac.managers.HmacManager;
import uk.gov.hmcts.cp.openapi.model.EventNotificationPayload;
import uk.gov.hmcts.cp.openapi.model.EventPayload;
import uk.gov.hmcts.cp.servicebus.config.ServiceBusProperties;
import uk.gov.hmcts.cp.servicebus.services.ServiceBusClientService;
import uk.gov.hmcts.cp.subscription.entities.ClientEntity;
import uk.gov.hmcts.cp.subscription.entities.ClientHmacEntity;
import uk.gov.hmcts.cp.subscription.mappers.NotificationMapper;
import uk.gov.hmcts.cp.subscription.model.EventNotificationPayloadWrapper;
import uk.gov.hmcts.cp.subscription.repositories.ClientHmacRepository;
import uk.gov.hmcts.cp.subscription.repositories.ClientEventRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.cp.subscription.services.CallbackDeliveryService.EXAMPLE_ENDPOINT;

@ExtendWith(MockitoExtension.class)
class CallbackDeliveryServiceTest {
    @Mock
    ServiceBusProperties serviceBusConfigService;
    @Mock
    JsonMapper jsonMapper;
    @Mock
    ClientEventRepository clientEventRepository;
    @Mock
    ClientHmacRepository clientHmacRepository;
    @Mock
    NotificationMapper notificationMapper;
    @Mock
    CallbackService callbackService;
    @Mock
    HmacManager hmacManager;
    @Mock
    ServiceBusClientService serviceBusClientService;

    @InjectMocks
    private CallbackDeliveryService callbackDeliveryService;

    private UUID documentId = randomUUID();
    private String callbackUrl = "https://callback.example.com";
    private UUID subscriptionId = randomUUID();
    private String hmacKeyId = "kid-v1";
    private ClientEntity clientEntity = ClientEntity.builder().subscriptionId(subscriptionId).callbackUrl(callbackUrl).build();
    private ClientHmacEntity clientHmacEntity = ClientHmacEntity.builder().keyId(hmacKeyId).build();
    private EventPayload eventPayload = EventPayload.builder().eventType("PRISON_COURT_REGISTER_GENERATED").build();
    private EventNotificationPayload payload = EventNotificationPayload.builder().build();
    private EventNotificationPayloadWrapper payloadWrapper = EventNotificationPayloadWrapper.builder().build();

    @Test
    void submit_should_send_when_servicebus_disabled() {
        when(clientEventRepository.findClientsByEventType("PRISON_COURT_REGISTER_GENERATED")).thenReturn(List.of(clientEntity));
        when(notificationMapper.mapToPayload(documentId, eventPayload)).thenReturn(payload);
        when(clientHmacRepository.findBySubscriptionId(subscriptionId)).thenReturn(Optional.of(clientHmacEntity));
        when(jsonMapper.toJson(payload)).thenReturn("{payload}");
        when(hmacManager.calculateSignature(hmacKeyId, "{payload}")).thenReturn("signature");
        when(notificationMapper.mapToWrapper(payload, hmacKeyId, "signature")).thenReturn(payloadWrapper);

        callbackDeliveryService.submitOutboundEvents(eventPayload, documentId);

        verify(callbackService).sendToSubscriber(callbackUrl, payloadWrapper);
    }

    @Test
    void submit_should_skip_when_example_endpoint() {
        ClientEntity clientWithExample = ClientEntity.builder().subscriptionId(subscriptionId).callbackUrl(EXAMPLE_ENDPOINT).build();
        when(clientEventRepository.findClientsByEventType("PRISON_COURT_REGISTER_GENERATED")).thenReturn(List.of(clientWithExample));
        when(notificationMapper.mapToPayload(documentId, eventPayload)).thenReturn(payload);
        when(clientHmacRepository.findBySubscriptionId(subscriptionId)).thenReturn(Optional.of(clientHmacEntity));
        when(jsonMapper.toJson(payload)).thenReturn("{payload}");
        when(hmacManager.calculateSignature(hmacKeyId, "{payload}")).thenReturn("signature");
        when(notificationMapper.mapToWrapper(payload, hmacKeyId, "signature")).thenReturn(payloadWrapper);

        callbackDeliveryService.submitOutboundEvents(eventPayload, documentId);

        verify(callbackService, never()).sendToSubscriber(anyString(), any(EventNotificationPayloadWrapper.class));
    }

    @Test
    void submit_async_should_add_to_queue() {
        when(serviceBusConfigService.isEnabled()).thenReturn(true);
        when(clientEventRepository.findClientsByEventType("PRISON_COURT_REGISTER_GENERATED")).thenReturn(List.of(clientEntity));
        when(notificationMapper.mapToPayload(documentId, eventPayload)).thenReturn(payload);
        when(clientHmacRepository.findBySubscriptionId(subscriptionId)).thenReturn(Optional.of(clientHmacEntity));
        when(jsonMapper.toJson(payload)).thenReturn("{payload}");
        when(jsonMapper.toJson(payloadWrapper)).thenReturn("{payload-wrapper}");
        when(hmacManager.calculateSignature(hmacKeyId, "{payload}")).thenReturn("signature");
        when(notificationMapper.mapToWrapper(payload, hmacKeyId, "signature")).thenReturn(payloadWrapper);

        callbackDeliveryService.submitOutboundEvents(eventPayload, documentId);

        verify(callbackService, never()).sendToSubscriber(anyString(), any(EventNotificationPayloadWrapper.class));}
}