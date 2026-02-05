package uk.gov.hmcts.cp.subscription.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.openapi.model.PcrEventPayload;
import uk.gov.hmcts.cp.subscription.entities.ClientSubscriptionEntity;
import uk.gov.hmcts.cp.subscription.mappers.SubscriberMapper;
import uk.gov.hmcts.cp.subscription.model.EntityEventType;
import uk.gov.hmcts.cp.subscription.model.PcrOutboundPayload;
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
        final PcrOutboundPayload pcrOutboundPayload = createPcrOutboundPayload(pcrEventPayload, documentId);
        for (final ClientSubscriptionEntity entity : entities) {
            final Subscriber subscriber = subscriberMapper.toSubscriber(entity);
            deliverToSubscriber(subscriber, documentId, pcrOutboundPayload);
        }
    }

    private void deliverToSubscriber(final Subscriber subscriber, final UUID documentId, final PcrOutboundPayload pcrOutboundPayload) {
        final String callbackURL = subscriber.getNotificationEndpoint();
        callbackService.sendToSubscriber(callbackURL, pcrOutboundPayload);
        log.info("Subscriber {} notified via callbackUrl {} for documentId {}", subscriber.getId(), callbackURL, documentId);
    }

    private PcrOutboundPayload createPcrOutboundPayload(final PcrEventPayload pcrEventPayload,
                                                        final UUID documentId) {
        return PcrOutboundPayload.builder()
                .eventId(pcrEventPayload.getEventId())
                .eventType(pcrEventPayload.getEventType())
                .documentId(documentId)
                .timestamp(pcrEventPayload.getTimestamp())
                //TBD - post to api changes
                .defendant(pcrEventPayload.getDefendant())
                .build();
    }
}
