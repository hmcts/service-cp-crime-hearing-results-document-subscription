package uk.gov.hmcts.cp.subscription.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import uk.gov.hmcts.cp.openapi.model.EventType;
import uk.gov.hmcts.cp.openapi.model.PcrEventPayload;
import uk.gov.hmcts.cp.subscription.entities.ClientSubscriptionEntity;
import uk.gov.hmcts.cp.subscription.mappers.SubscriberMapper;
import uk.gov.hmcts.cp.subscription.model.Subscriber;
import uk.gov.hmcts.cp.subscription.model.PcrOutboundPayload;
import uk.gov.hmcts.cp.subscription.repositories.SubscriptionRepository;

import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;

/**
 * Handles resolving subscribers by event type and delivering callbackURL with document URLs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CallbackDeliveryService {

    @Value("${subscription.service.base-url:}")
    private String subscriptionServiceBaseUrl;

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriberMapper subscriberMapper;
    private final CallbackService callbackService;

    /**
     * Resolves subscribers for the event type and delivers a callbackUrl to each with the document URL.
     */
    public void processPcrEvent(final PcrEventPayload pcrEventPayload, final UUID documentId) throws JsonProcessingException, URISyntaxException {
        final String eventTypeName = pcrEventPayload.getEventType().name();

        if (eventTypeName.contains("CUSTODIAL_RESULT")) {
            throw new UnsupportedOperationException("CUSTODIAL_RESULT not implemented");
        }
        if (eventTypeName.contains("PRISON_COURT_REGISTER_GENERATED")) {
            deliverForPCREvent(EventType.PRISON_COURT_REGISTER_GENERATED.name(), pcrEventPayload, documentId);
        }
    }

    private void deliverForPCREvent(final String eventTypeName,
                                    final PcrEventPayload pcrEventPayload,
                                    final UUID documentId) throws JsonProcessingException, URISyntaxException {

        final List<ClientSubscriptionEntity> entities = subscriptionRepository.findByEventType(eventTypeName);
        if (entities.isEmpty()) {
            return;
        }
        final PcrOutboundPayload pcrOutboundPayload = getPcrOutboundPayload(pcrEventPayload, documentId);

        for (final ClientSubscriptionEntity entity : entities) {
            final Subscriber subscriber = subscriberMapper.toSubscriber(entity);
            deliverToSubscriber(subscriber, documentId, pcrOutboundPayload.toString());
        }
    }

    private void deliverToSubscriber(final Subscriber subscriber, final UUID documentId, final String pcrOutboundPayload) throws JsonProcessingException, URISyntaxException {
        final String callbackURL = subscriber.getNotificationEndpoint();
        callbackService.post(callbackURL, pcrOutboundPayload);
        log.info("PCR - callbackUrl delivered to subscriber {} for documentId {}", subscriber.getId(), documentId);
    }

    private static PcrOutboundPayload getPcrOutboundPayload(final PcrEventPayload pcrEventPayload, final UUID documentId) {
       return PcrOutboundPayload.builder()
                .pcrEventPayload(pcrEventPayload)
                .documentId(documentId.toString())
                .build();
    }
}
