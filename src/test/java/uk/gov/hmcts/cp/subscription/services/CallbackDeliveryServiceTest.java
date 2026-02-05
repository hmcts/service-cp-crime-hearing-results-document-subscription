package uk.gov.hmcts.cp.subscription.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cp.openapi.model.EventType;
import uk.gov.hmcts.cp.openapi.model.PcrEventPayload;
import uk.gov.hmcts.cp.openapi.model.PcrEventPayloadDefendant;
import uk.gov.hmcts.cp.subscription.entities.ClientSubscriptionEntity;
import uk.gov.hmcts.cp.subscription.mappers.SubscriberMapper;
import uk.gov.hmcts.cp.subscription.model.EntityEventType;
import uk.gov.hmcts.cp.subscription.model.PcrOutboundPayload;
import uk.gov.hmcts.cp.subscription.model.Subscriber;
import uk.gov.hmcts.cp.subscription.repositories.SubscriptionRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CallbackDeliveryServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private SubscriberMapper subscriberMapper;

    @Mock
    private CallbackService callbackService;

    @Mock
    private PcrEventPayload pcrEventPayload;

    @Mock
    private PcrEventPayloadDefendant defendant;

    @InjectMocks
    private CallbackDeliveryService callbackDeliveryService;

    private static final UUID DOCUMENT_ID = randomUUID();

    @Test
    void processPcrEvent_custodialResult_shouldThrowUnsupportedOperation() {
        when(pcrEventPayload.getEventType()).thenReturn(EventType.CUSTODIAL_RESULT);

        assertThrows(UnsupportedOperationException.class,
                () -> callbackDeliveryService.processPcrEvent(pcrEventPayload, DOCUMENT_ID));

        verifyNoInteractions(subscriptionRepository, subscriberMapper, callbackService);
    }

    @Test
    void processPcrEvent_shouldMapOutboundPayloadFieldsCorrectly() {
        UUID eventId = randomUUID();
        Instant timestamp = Instant.now();

        when(pcrEventPayload.getEventType()).thenReturn(EventType.PRISON_COURT_REGISTER_GENERATED);
        when(pcrEventPayload.getEventId()).thenReturn(eventId);
        when(pcrEventPayload.getTimestamp()).thenReturn(timestamp);
        when(pcrEventPayload.getDefendant()).thenReturn(defendant);

        ClientSubscriptionEntity entity = ClientSubscriptionEntity.builder()
                .id(randomUUID())
                .notificationEndpoint("https://callback.example.com")
                .eventTypes(List.of(EntityEventType.PRISON_COURT_REGISTER_GENERATED))
                .build();

        when(subscriptionRepository.findByEventType(EntityEventType.PRISON_COURT_REGISTER_GENERATED.name()))
                .thenReturn(List.of(entity));

        Subscriber subscriber = Subscriber.builder()
                .id(entity.getId())
                .notificationEndpoint(entity.getNotificationEndpoint())
                .eventTypes(entity.getEventTypes())
                .clientId(randomUUID())
                .build();

        when(subscriberMapper.toSubscriber(entity)).thenReturn(subscriber);

        ArgumentCaptor<PcrOutboundPayload> payloadCaptor = ArgumentCaptor.forClass(PcrOutboundPayload.class);
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);

        callbackDeliveryService.processPcrEvent(pcrEventPayload, DOCUMENT_ID);

        verify(callbackService).sendToSubscriber(urlCaptor.capture(), payloadCaptor.capture());

        assertThat(urlCaptor.getValue()).isEqualTo("https://callback.example.com");

        PcrOutboundPayload outbound = payloadCaptor.getValue();
        assertThat(outbound.getEventId()).isEqualTo(eventId);
        assertThat(outbound.getEventType()).isEqualTo(EventType.PRISON_COURT_REGISTER_GENERATED);
        assertThat(outbound.getDocumentId()).isEqualTo(DOCUMENT_ID);
        assertThat(outbound.getTimestamp()).isEqualTo(timestamp);
        assertThat(outbound.getDefendant()).isEqualTo(defendant);
    }
}