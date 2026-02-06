package uk.gov.hmcts.cp.subscription.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.openapi.model.EventNotificationPayload;
import uk.gov.hmcts.cp.openapi.model.EventNotificationPayloadCasesInner;
import uk.gov.hmcts.cp.openapi.model.PcrEventPayload;
import uk.gov.hmcts.cp.openapi.model.PcrEventPayloadDefendant;
import uk.gov.hmcts.cp.subscription.entities.ClientSubscriptionEntity;
import uk.gov.hmcts.cp.subscription.mappers.SubscriberMapper;
import uk.gov.hmcts.cp.subscription.model.EntityEventType;
import uk.gov.hmcts.cp.subscription.model.Subscriber;
import uk.gov.hmcts.cp.subscription.repositories.SubscriptionRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CallbackDeliveryService {

    @Value("${subscription.service.base-url:}")
    private String subscriptionServiceBaseUrl;

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriberMapper subscriberMapper;
    private final CallbackService callbackService;

    public void processPcrEvent(final PcrEventPayload pcrEventPayload, final UUID documentId) {
        final EntityEventType eventType = EntityEventType.valueOf(pcrEventPayload.getEventType().name());
        deliverForPCREvent(eventType, pcrEventPayload, documentId);
    }

    private void deliverForPCREvent(final EntityEventType eventType,
                                    final PcrEventPayload pcrEventPayload,
                                    final UUID documentId) {
        if (eventType == EntityEventType.CUSTODIAL_RESULT) {
            throw new UnsupportedOperationException("CUSTODIAL_RESULT not implemented");
        }
        final List<ClientSubscriptionEntity> entities = subscriptionRepository.findByEventType(eventType.name());
        final EventNotificationPayload eventNotificationPayload = createEventNotificationPayload(pcrEventPayload, documentId);
        for (final ClientSubscriptionEntity entity : entities) {
            final Subscriber subscriber = subscriberMapper.toSubscriber(entity);
            deliverToSubscriber(subscriber, eventNotificationPayload);
        }
    }

    private void deliverToSubscriber(final Subscriber subscriber, final EventNotificationPayload eventNotificationPayload) {
        final String callbackURL = subscriber.getNotificationEndpoint();
        callbackService.sendToSubscriber(callbackURL, eventNotificationPayload);
        log.info("Subscriber {} notified via callbackUrl {} for documentId {}", subscriber.getId(), callbackURL, eventNotificationPayload.getDocumentId());
    }

    private EventNotificationPayload createEventNotificationPayload(final PcrEventPayload pcrEventPayload,
                                                                   final UUID documentId) {
        final PcrEventPayloadDefendant defendant = pcrEventPayload.getDefendant();
        final List<EventNotificationPayloadCasesInner> cases = defendant.getCases().stream()
                .map(c -> EventNotificationPayloadCasesInner.builder().urn(c.getUrn()).build())
                .toList();
        final String prisonEmailAddress = defendant.getCustodyEstablishmentDetails().getEmailAddress();
        return EventNotificationPayload.builder()
                .cases(cases)
                .masterDefendantId(defendant.getMasterDefendantId())
                .defendantName(defendant.getName())
                .defendantDateOfBirth(defendant.getDateOfBirth())
                .documentId(documentId)
                .documentGeneratedTimestamp(pcrEventPayload.getTimestamp())
                .prisonEmailAddress(prisonEmailAddress)
                .build();
    }
}
