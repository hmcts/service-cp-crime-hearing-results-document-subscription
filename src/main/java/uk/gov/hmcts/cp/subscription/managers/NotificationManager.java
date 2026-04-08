package uk.gov.hmcts.cp.subscription.managers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.cp.openapi.model.EventNotificationPayload;
import uk.gov.hmcts.cp.openapi.model.EventPayload;
import uk.gov.hmcts.cp.subscription.model.DocumentContent;
import uk.gov.hmcts.cp.subscription.services.CallbackDeliveryService;
import uk.gov.hmcts.cp.subscription.services.DocumentService;
import uk.gov.hmcts.cp.subscription.services.NotificationService;
import uk.gov.hmcts.cp.subscription.services.SubscriptionService;

import java.util.UUID;

/**
 * Orchestrates notification and document retrieval flows.
 * Encapsulates coordination between notification, subscription, document and callback services.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationManager {

    private final NotificationService notificationService;
    private final DocumentService documentService;
    private final SubscriptionService subscriptionService;
    private final CallbackDeliveryService callbackDeliveryService;

    public EventNotificationPayload processPcrNotification(final EventPayload eventPayload) {
        log.info("processPcrNotification eventId:{} materialId:{}", eventPayload.getEventId(), eventPayload.getMaterialId());
        final UUID documentId = notificationService.processInboundEvent(eventPayload);
        final EventNotificationPayload payload = callbackDeliveryService.submitOutboundPcrEvents(eventPayload, documentId);
        log.info("processPcrNotification complete eventId:{} documentId:{}", eventPayload.getEventId(), documentId);
        return payload;
    }

    public DocumentContent getPcrDocumentContent(final UUID clientSubscriptionId, final UUID documentId) {
        log.info("getPcrDocumentContent clientSubscriptionId:{} documentId:{}", clientSubscriptionId, documentId);
        final String eventType = documentService.getEventTypeForDocument(documentId);
        if (!subscriptionService.hasAccess(clientSubscriptionId, eventType)) {
            log.warn("getPcrDocumentContent access denied clientSubscriptionId:{} eventType:{}", clientSubscriptionId, eventType);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: subscription does not have access to this document");
        }
        return documentService.getDocumentContent(documentId);
    }
}
