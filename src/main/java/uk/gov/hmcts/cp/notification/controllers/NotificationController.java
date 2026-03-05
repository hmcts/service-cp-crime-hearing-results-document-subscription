package uk.gov.hmcts.cp.notification.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;
import uk.gov.hmcts.cp.filter.ClientIdResolutionFilter;
import uk.gov.hmcts.cp.notification.managers.NotificationManager;
import uk.gov.hmcts.cp.openapi.api.InternalApi;
import uk.gov.hmcts.cp.openapi.api.NotificationApi;
import uk.gov.hmcts.cp.openapi.model.PcrEventPayload;
import uk.gov.hmcts.cp.subscription.model.DocumentContent;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles PCR notification events and document retrieval for subscribers.
 * Delegates orchestration to NotificationManager; builds HTTP responses only.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class NotificationController implements InternalApi, NotificationApi {

    private final NotificationManager notificationManager;
    private final HttpServletRequest httpRequest;

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    @Override
    public ResponseEntity<Void> createNotificationPCR(@Valid @RequestBody final PcrEventPayload pcrEventPayload) {
        log.info("PCR - Received PCR notification request from Progression Service - eventId: {}, materialId: {}, eventType: {}",
                pcrEventPayload.getEventId(),
                pcrEventPayload.getMaterialId(),
                pcrEventPayload.getEventType());
        // TODO should we get a separate correlationId ? Should we use a cross cutting approach ?
        notificationManager.processPcrNotification(pcrEventPayload);
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @Override
    public ResponseEntity<Resource> getDocument(
            @NotNull @PathVariable("clientSubscriptionId") final UUID clientSubscriptionId,
            @NotNull @PathVariable("documentId") final UUID documentId) {
        final UUID clientId = UUID.fromString(MDC.get(ClientIdResolutionFilter.MDC_CLIENT_ID));
        final DocumentContent content = notificationManager.getPcrDocumentContent(clientSubscriptionId, clientId, documentId);
        final Resource resource = new ByteArrayResource(content.getBody());
        final HttpHeaders headers = getHttpHeaders(content);
        return new ResponseEntity<>(resource, headers, HttpStatus.OK);
    }

    private @NonNull HttpHeaders getHttpHeaders(final DocumentContent content) {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(content.getContentType());
        headers.setContentLength(content.getBody().length);
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + encodeFileName(content.getFileName()) + "\"");
        return headers;
    }

    private String encodeFileName(final String fileName) {
        return URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", " ");
    }
}
