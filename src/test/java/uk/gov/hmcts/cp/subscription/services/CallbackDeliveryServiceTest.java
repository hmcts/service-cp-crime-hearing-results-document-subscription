package uk.gov.hmcts.cp.subscription.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cp.openapi.model.EventType;
import uk.gov.hmcts.cp.openapi.model.PcrEventPayload;
import uk.gov.hmcts.cp.subscription.entities.ClientSubscriptionEntity;
import uk.gov.hmcts.cp.subscription.mappers.SubscriberMapper;
import uk.gov.hmcts.cp.subscription.model.EntityEventType;
import uk.gov.hmcts.cp.subscription.model.Subscriber;
import uk.gov.hmcts.cp.subscription.repositories.SubscriptionRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CallbackDeliveryServiceTest {
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private SubscriberMapper subscriberMapper;
    @Mock private CallbackService callbackService;
    @Mock private PcrEventPayload pcrEventPayload;

    @InjectMocks
    private CallbackDeliveryService callbackDeliveryService;

    private static final UUID DOCUMENT_ID = randomUUID();
    private static final UUID EVENT_ID = randomUUID();
    private static final String CALLBACK_URL = "https://callback.example.com";
    private static Instant timestamp = Instant.now();
    private static final PcrEventPayload PCR_EVENT_PAYLOAD = PcrEventPayload.builder()
            .eventType(EventType.PRISON_COURT_REGISTER_GENERATED)
            .eventId(EVENT_ID)
            .timestamp(timestamp)
            .defendant(null) //TODO verify for few more params after latest api changes
            .build();
    private static final ClientSubscriptionEntity SUB_ENTITY = ClientSubscriptionEntity.builder()
            .id(randomUUID())
            .notificationEndpoint(CALLBACK_URL)
            .eventTypes(List.of(EntityEventType.PRISON_COURT_REGISTER_GENERATED))
            .build();
    private static final Subscriber SUBSCRIBER = Subscriber.builder()
            .id(SUB_ENTITY.getId())
            .notificationEndpoint(CALLBACK_URL)
            .eventTypes(SUB_ENTITY.getEventTypes())
            .clientId(randomUUID())
            .build();

    @Test
    void processPcrEvent_custodialResult_shouldThrowUnsupportedOperation() {
        when(pcrEventPayload.getEventType()).thenReturn(EventType.CUSTODIAL_RESULT);

        assertThrows(UnsupportedOperationException.class,
                () -> callbackDeliveryService.processPcrEvent(pcrEventPayload, DOCUMENT_ID));

        verifyNoInteractions(subscriptionRepository, subscriberMapper, callbackService);
    }
    //TODO verify for few more params after latest api changes
    @Test
    void processPcrEvent_shouldMapOutboundPayloadFieldsCorrectly() {
        when(subscriptionRepository.findByEventType(EntityEventType.PRISON_COURT_REGISTER_GENERATED.name()))
                .thenReturn(List.of(SUB_ENTITY));
        when(subscriberMapper.toSubscriber(SUB_ENTITY)).thenReturn(SUBSCRIBER);

        callbackDeliveryService.processPcrEvent(PCR_EVENT_PAYLOAD, DOCUMENT_ID);

        verify(callbackService).sendToSubscriber(eq(CALLBACK_URL), argThat(p ->
                p.getEventId().equals(EVENT_ID)
                        && p.getEventType() == EventType.PRISON_COURT_REGISTER_GENERATED
                        && p.getDocumentId().equals(DOCUMENT_ID)
                        && p.getTimestamp().equals(timestamp)
                        && p.getDefendant() == null));
    }
}