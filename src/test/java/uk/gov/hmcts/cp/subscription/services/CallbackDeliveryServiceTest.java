package uk.gov.hmcts.cp.subscription.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cp.vault.model.KeyPair;
import uk.gov.hmcts.cp.vault.services.VaultKeyService;
import uk.gov.hmcts.cp.vault.services.VaultSigningService;
import uk.gov.hmcts.cp.openapi.model.EventNotificationPayload;
import uk.gov.hmcts.cp.openapi.model.EventPayload;
import uk.gov.hmcts.cp.servicebus.config.ServiceBusConfigService;
import uk.gov.hmcts.cp.subscription.entities.ClientSubscriptionEntity;
import uk.gov.hmcts.cp.subscription.mappers.NotificationMapper;
import uk.gov.hmcts.cp.subscription.mappers.SubscriberMapper;
import uk.gov.hmcts.cp.subscription.model.EventNotificationPayloadWrapper;
import uk.gov.hmcts.cp.subscription.model.Subscriber;
import uk.gov.hmcts.cp.subscription.repositories.SubscriptionRepository;

import java.util.List;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CallbackDeliveryServiceTest {
    @Mock
    ServiceBusConfigService serviceBusConfigService;
    @Mock
    JsonMapper jsonMapper;
    @Mock
    SubscriptionRepository subscriptionRepository;
    @Mock
    NotificationMapper notificationMapper;
    @Mock
    SubscriberMapper subscriberMapper;
    @Mock
    CallbackService callbackService;
    @Mock
    VaultKeyService vaultKeyService;
    @Mock
    VaultSigningService vaultSigningService;

    @InjectMocks
    private CallbackDeliveryService callbackDeliveryService;

    private UUID documentId = randomUUID();
    private String callbackUrl = "https://callback.example.com";
    private ClientSubscriptionEntity subscriptionEntity = ClientSubscriptionEntity.builder().build();
    private EventPayload eventPayload = EventPayload.builder().eventType("PRISON_COURT_REGISTER_GENERATED").build();
    private Subscriber subscriber = Subscriber.builder().notificationEndpoint(callbackUrl).build();
    private EventNotificationPayload payload = EventNotificationPayload.builder().build();
    private EventNotificationPayloadWrapper payloadWrapper = EventNotificationPayloadWrapper.builder().build();
    private KeyPair keyPair = KeyPair.builder().keyId("keyId").secret("secret".getBytes()).build();

    @Test
    void submit_should_send_when_servicebus_disabled() {
        when(subscriptionRepository.findByEventType("PRISON_COURT_REGISTER_GENERATED")).thenReturn(List.of(subscriptionEntity));
        when(notificationMapper.mapToPayload(documentId, eventPayload)).thenReturn(payload);
        when(subscriberMapper.toSubscriber(subscriptionEntity)).thenReturn(subscriber);
        when(jsonMapper.toJson(payload)).thenReturn("{payload}");
        when(vaultKeyService.getKeyPair(subscriptionEntity.getId())).thenReturn(keyPair);
        when(vaultSigningService.sign("secret".getBytes(), "{payload}")).thenReturn("signature");
        when(notificationMapper.mapToWrapper(payload, "keyId", "signature")).thenReturn(payloadWrapper);

        callbackDeliveryService.submitOutboundPcrEvents(eventPayload, documentId);

        verify(callbackService).sendToSubscriber(callbackUrl, payloadWrapper);
    }

    void submit_should_send_when_servicebus_enabled() {
        when(serviceBusConfigService.isEnabled()).thenReturn(true);

        callbackDeliveryService.submitOutboundPcrEvents(eventPayload, documentId);
    }
}