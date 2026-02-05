package uk.gov.hmcts.cp.subscription.managers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.cp.openapi.model.PcrEventPayload;
import uk.gov.hmcts.cp.subscription.model.DocumentContent;
import uk.gov.hmcts.cp.subscription.model.EntityEventType;
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

    public void processPcrNotification(final PcrEventPayload pcrEventPayload) {
        notificationService.processInboundEvent(pcrEventPayload);

        final UUID materialId = pcrEventPayload.getMaterialId();
        final EntityEventType eventType = EntityEventType.valueOf(pcrEventPayload.getEventType().name());
        final UUID documentId = documentService.getDocumentIdForMaterialId(materialId, eventType);
        callbackDeliveryService.processPcrEvent(pcrEventPayload, documentId);
    }

    public DocumentContent getPcrDocumentContent(final UUID clientSubscriptionId, final UUID documentId) {
        final EntityEventType eventType = documentService.getEventTypeForDocument(documentId);
        if (!subscriptionService.hasAccess(clientSubscriptionId, eventType)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: subscription does not have access to this document");
        }
        return documentService.getDocumentContent(documentId);
    }
}
