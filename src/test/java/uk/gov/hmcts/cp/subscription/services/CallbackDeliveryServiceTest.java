package uk.gov.hmcts.cp.subscription.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cp.openapi.model.EventType;
import uk.gov.hmcts.cp.openapi.model.PcrEventPayload;
import uk.gov.hmcts.cp.openapi.model.PcrEventPayloadDefendant;
import uk.gov.hmcts.cp.openapi.model.PcrEventPayloadDefendantCasesInner;
import uk.gov.hmcts.cp.openapi.model.PcrEventPayloadDefendantCustodyEstablishmentDetails;
import uk.gov.hmcts.cp.subscription.entities.ClientSubscriptionEntity;
import uk.gov.hmcts.cp.subscription.mappers.SubscriberMapper;
import uk.gov.hmcts.cp.subscription.model.EntityEventType;
import uk.gov.hmcts.cp.subscription.model.Subscriber;
import uk.gov.hmcts.cp.subscription.repositories.SubscriptionRepository;

import java.time.Instant;
import java.time.LocalDate;
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
    private static final UUID MASTER_DEFENDANT_ID = randomUUID();
    private static final String CALLBACK_URL = "https://callback.example.com";
    private static final Instant TIMESTAMP = Instant.EPOCH;
    private static final String CASE_URN = "CT98KRYCAP";
    private static final String PRISON_EMAIL = "prison@example.com";
    private static final PcrEventPayloadDefendant DEFENDANT = PcrEventPayloadDefendant.builder()
            .masterDefendantId(MASTER_DEFENDANT_ID)
            .name("John Doe")
            .dateOfBirth(LocalDate.of(1990, 5, 15))
            .custodyEstablishmentDetails(PcrEventPayloadDefendantCustodyEstablishmentDetails.builder()
                    .emailAddress(PRISON_EMAIL)
                    .build())
            .cases(List.of(PcrEventPayloadDefendantCasesInner.builder().urn(CASE_URN).build()))
            .build();
    private static final PcrEventPayload PCR_EVENT_PAYLOAD = PcrEventPayload.builder()
            .eventType(EventType.PRISON_COURT_REGISTER_GENERATED)
            .eventId(EVENT_ID)
            .timestamp(TIMESTAMP)
            .defendant(DEFENDANT)
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
    @Test
    void processPcrEvent_shouldMapEventNotificationPayloadFieldsCorrectly() {
        when(subscriptionRepository.findByEventType(EntityEventType.PRISON_COURT_REGISTER_GENERATED.name()))
                .thenReturn(List.of(SUB_ENTITY));
        when(subscriberMapper.toSubscriber(SUB_ENTITY)).thenReturn(SUBSCRIBER);

        callbackDeliveryService.processPcrEvent(PCR_EVENT_PAYLOAD, DOCUMENT_ID);

        verify(callbackService).sendToSubscriber(eq(CALLBACK_URL), argThat(p ->
                p.getCases().size() == 1 && p.getCases().get(0).getUrn().equals(CASE_URN)
                        && p.getMasterDefendantId().equals(MASTER_DEFENDANT_ID)
                        && p.getDefendantName().equals("John Doe")
                        && p.getDefendantDateOfBirth().equals(LocalDate.of(1990, 5, 15))
                        && p.getDocumentId().equals(DOCUMENT_ID)
                        && p.getDocumentGeneratedTimestamp().equals(TIMESTAMP)
                        && p.getPrisonEmailAddress().equals(PRISON_EMAIL)));
    }
}