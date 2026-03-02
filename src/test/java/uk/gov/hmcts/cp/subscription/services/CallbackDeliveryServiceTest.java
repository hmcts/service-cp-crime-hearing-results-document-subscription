package uk.gov.hmcts.cp.subscription.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cp.openapi.model.EventNotificationPayload;
import uk.gov.hmcts.cp.openapi.model.EventType;
import uk.gov.hmcts.cp.openapi.model.PcrEventPayload;
import uk.gov.hmcts.cp.servicebus.config.ServiceBusConfigService;
import uk.gov.hmcts.cp.subscription.entities.ClientSubscriptionEntity;
import uk.gov.hmcts.cp.subscription.mappers.NotificationMapper;
import uk.gov.hmcts.cp.subscription.mappers.SubscriberMapper;
import uk.gov.hmcts.cp.subscription.model.EntityEventType;
import uk.gov.hmcts.cp.subscription.model.Subscriber;
import uk.gov.hmcts.cp.subscription.repositories.SubscriptionRepository;

import java.util.List;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CallbackDeliveryServiceTest {
    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private NotificationMapper notificationMapper;
    @Mock
    private SubscriberMapper subscriberMapper;
    @Mock
    private CallbackService callbackService;
    @Mock
    private ServiceBusConfigService serviceBusConfigService;

    @InjectMocks
    private CallbackDeliveryService callbackDeliveryService;

    private final UUID correlationId = randomUUID();
    private final UUID documentId = randomUUID();
    private final String callbackUrl = "https://callback.example.com";
    private final ClientSubscriptionEntity subscriptionEntity = ClientSubscriptionEntity.builder().build();
    private final Subscriber subscriber = Subscriber.builder().notificationEndpoint(callbackUrl).build();
    private final EventNotificationPayload eventNotificationPayload = EventNotificationPayload.builder().build();

    @Test
    void processPcrEvent_custodialResult_shouldThrowUnsupportedOperation() {
        PcrEventPayload pcrEventPayload = PcrEventPayload.builder().eventType(EventType.CUSTODIAL_RESULT).build();
        assertThrows(UnsupportedOperationException.class, () -> callbackDeliveryService.processPcrEvent(pcrEventPayload, documentId));
        verifyNoInteractions(subscriptionRepository, subscriberMapper, callbackService);
    }

    @Test
    void processPcrEvent_shouldMapEventNotificationPayloadFieldsCorrectly() {
        PcrEventPayload pcrEventPayload = PcrEventPayload.builder().eventType(EventType.PRISON_COURT_REGISTER_GENERATED).build();
        when(subscriptionRepository.findByEventType(EntityEventType.PRISON_COURT_REGISTER_GENERATED.name())).thenReturn(List.of(subscriptionEntity));
        when(notificationMapper.mapToPayload(documentId, pcrEventPayload)).thenReturn(eventNotificationPayload);
        when(subscriberMapper.toSubscriber(subscriptionEntity)).thenReturn(subscriber);

        callbackDeliveryService.processPcrEvent(pcrEventPayload, documentId);

        verify(callbackService).sendToSubscriber(callbackUrl, eventNotificationPayload);
    }
}