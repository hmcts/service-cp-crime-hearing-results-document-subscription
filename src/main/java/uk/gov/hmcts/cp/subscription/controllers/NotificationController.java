package uk.gov.hmcts.cp.subscription.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import uk.gov.hmcts.cp.openapi.api.NotificationApi;
import uk.gov.hmcts.cp.openapi.model.PcrEventPayload;
import uk.gov.hmcts.cp.subscription.model.DocumentContent;
import uk.gov.hmcts.cp.subscription.model.EntityEventType;
import uk.gov.hmcts.cp.subscription.services.DocumentService;
import uk.gov.hmcts.cp.subscription.services.NotificationService;
import uk.gov.hmcts.cp.subscription.services.CallbackDeliveryService;
import uk.gov.hmcts.cp.subscription.services.exceptions.CallbackUrlDeliveryException;

import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Handles PCR notification events and document retrieval for subscribers.
 * Implements NotificationApi with endpoints for receiving PCR events and serving document content.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class NotificationController implements NotificationApi {

    private final NotificationService notificationService;
    private final DocumentService documentService;
    private final CallbackDeliveryService callbackDeliveryService;

    /**
     * Processes incoming Prison Court Register (PCR) notification events from the Progression Service.
     * Validates material metadata availability, creates document mappings, and delivers callback
     * notifications to all active subscribers for the event type.
     */
    @Override
    public ResponseEntity<Void> createNotificationPCR(@Valid @RequestBody final PcrEventPayload pcrEventPayload) {
        log.info("PCR - Received PCR notification request from Progression Service - eventId: {}, materialId: {}, eventType: {}",
                pcrEventPayload.getEventId(),
                pcrEventPayload.getMaterialId(),
                pcrEventPayload.getEventType());

        notificationService.processPcrEvent(pcrEventPayload);

        final UUID materialId = convertToUuid(pcrEventPayload.getMaterialId());
        final EntityEventType eventType = EntityEventType.valueOf(pcrEventPayload.getEventType().name());
        final UUID documentId = documentService.getDocumentIdForMaterialId(materialId, eventType);

        try {
            callbackDeliveryService.processPcrEvent(pcrEventPayload, documentId);
        } catch (JsonProcessingException | URISyntaxException e) {
            throw new CallbackUrlDeliveryException("PCR - Failed to build or deliver callback payload: " + e.getMessage(), e);
        }
        
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    /**
     * Retrieves PCR document content in binary format for a specific client subscription and document.
     * Validates that the subscription has an active subscription for the document's event type,
     * fetches the binary content from the Material Service contentUrl (e.g., Azure Blob Storage),
     * and returns it with appropriate headers for file download.
     */
    @Override
    public ResponseEntity<Resource> getPcrDocumentByClientSubscription(
            @PathVariable final UUID clientSubscriptionId,
            @PathVariable final UUID documentId) {
        final DocumentContent content = documentService.getDocumentContentAsBinary(clientSubscriptionId, documentId);
        final Resource resource = new ByteArrayResource(content.getBody());
        final HttpHeaders headers = getHttpHeaders(content);
        return new ResponseEntity<>(resource, headers, HttpStatus.OK);
    }

    private static @NonNull HttpHeaders getHttpHeaders(final DocumentContent content) {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(content.getContentType()));
        headers.setContentLength(content.getBody().length);
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + encodeFileName(content.getFileName()) + "\"");
        return headers;
    }

    private static UUID convertToUuid(final Object value) {
        return value == null ? null : value instanceof UUID ? (UUID) value : UUID.fromString(value.toString());
    }

    private static String encodeFileName(final String fileName) {
        return URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", " ");
    }
}
