package uk.gov.hmcts.cp.subscription.unit.managers;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.cp.openapi.model.EventType;
import uk.gov.hmcts.cp.openapi.model.PcrEventPayload;
import uk.gov.hmcts.cp.subscription.managers.NotificationManager;
import uk.gov.hmcts.cp.subscription.model.DocumentContent;
import uk.gov.hmcts.cp.subscription.model.EntityEventType;
import uk.gov.hmcts.cp.subscription.services.CallbackDeliveryService;
import uk.gov.hmcts.cp.subscription.services.DocumentService;
import uk.gov.hmcts.cp.subscription.services.NotificationService;
import uk.gov.hmcts.cp.subscription.services.SubscriptionService;
import uk.gov.hmcts.cp.subscription.services.exceptions.CallbackUrlDeliveryException;

import java.net.URISyntaxException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationManagerTest {

    private static final UUID MATERIAL_ID = UUID.randomUUID();
    private static final UUID DOCUMENT_ID = UUID.randomUUID();
    private static final UUID SUBSCRIPTION_ID = UUID.randomUUID();

    @Mock
    NotificationService notificationService;

    @Mock
    DocumentService documentService;

    @Mock
    SubscriptionService subscriptionService;

    @Mock
    CallbackDeliveryService callbackDeliveryService;

    @InjectMocks
    NotificationManager notificationManager;

    @Test
    void processPcrNotification_should_process_deliver_and_not_throw() throws Exception {
        PcrEventPayload payload = PcrEventPayload.builder()
                .materialId(MATERIAL_ID)
                .eventType(EventType.PRISON_COURT_REGISTER_GENERATED)
                .build();
        doNothing().when(notificationService).processInboundEvent(any(PcrEventPayload.class));
        when(documentService.getDocumentIdForMaterialId(eq(MATERIAL_ID), eq(EntityEventType.PRISON_COURT_REGISTER_GENERATED)))
                .thenReturn(DOCUMENT_ID);
        doNothing().when(callbackDeliveryService).processPcrEvent(any(PcrEventPayload.class), eq(DOCUMENT_ID));

        notificationManager.processPcrNotification(payload);

        verify(notificationService).processInboundEvent(eq(payload));
        verify(documentService).getDocumentIdForMaterialId(MATERIAL_ID, EntityEventType.PRISON_COURT_REGISTER_GENERATED);
        verify(callbackDeliveryService).processPcrEvent(eq(payload), eq(DOCUMENT_ID));
    }

    @Test
    void getPcrDocumentContent_should_return_content_when_subscription_has_access() {
        DocumentContent content = DocumentContent.builder()
                .body("PDF".getBytes())
                .contentType(MediaType.APPLICATION_PDF)
                .fileName("doc.pdf")
                .build();
        when(documentService.getEventTypeForDocument(DOCUMENT_ID)).thenReturn(EntityEventType.PRISON_COURT_REGISTER_GENERATED);
        when(subscriptionService.hasAccess(SUBSCRIPTION_ID, EntityEventType.PRISON_COURT_REGISTER_GENERATED)).thenReturn(true);
        when(documentService.getDocumentContent(DOCUMENT_ID)).thenReturn(content);

        DocumentContent result = notificationManager.getPcrDocumentContent(SUBSCRIPTION_ID, DOCUMENT_ID);

        assertThat(result).isEqualTo(content);
        verify(documentService).getEventTypeForDocument(DOCUMENT_ID);
        verify(subscriptionService).hasAccess(SUBSCRIPTION_ID, EntityEventType.PRISON_COURT_REGISTER_GENERATED);
        verify(documentService).getDocumentContent(DOCUMENT_ID);
    }

    @Test
    void getPcrDocumentContent_should_throw_forbidden_when_subscription_has_no_access() {
        when(documentService.getEventTypeForDocument(DOCUMENT_ID)).thenReturn(EntityEventType.PRISON_COURT_REGISTER_GENERATED);
        when(subscriptionService.hasAccess(SUBSCRIPTION_ID, EntityEventType.PRISON_COURT_REGISTER_GENERATED)).thenReturn(false);

        ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
                () -> notificationManager.getPcrDocumentContent(SUBSCRIPTION_ID, DOCUMENT_ID));

        assertThat(thrown.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(thrown.getReason()).contains("Access denied");
        verify(documentService).getEventTypeForDocument(DOCUMENT_ID);
        verify(subscriptionService).hasAccess(SUBSCRIPTION_ID, EntityEventType.PRISON_COURT_REGISTER_GENERATED);
        verify(documentService, never()).getDocumentContent(any());
    }
}
