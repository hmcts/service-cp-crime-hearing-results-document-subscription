package uk.gov.hmcts.cp.subscription.unit.controllers;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cp.openapi.model.PcrEventPayload;
import uk.gov.hmcts.cp.subscription.controllers.NotificationController;
import uk.gov.hmcts.cp.subscription.model.EntityEventType;
import uk.gov.hmcts.cp.subscription.services.DocumentService;
import uk.gov.hmcts.cp.subscription.services.NotificationService;
import uk.gov.hmcts.cp.subscription.services.CallbackDeliveryService;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    NotificationService notificationService;

    @Mock
    DocumentService documentService;

    @Mock
    CallbackDeliveryService callbackDeliveryService;

    @InjectMocks
    NotificationController notificationController;

    @SneakyThrows
    @Test
    void valid_pcr_payload_should_process_deliver_callbackurl_and_return_accepted() {
        PcrEventPayload payload = PcrEventPayload.builder()
                .materialId(UUID.randomUUID())
                .eventType(uk.gov.hmcts.cp.openapi.model.EventType.PRISON_COURT_REGISTER_GENERATED)
                .build();
        doNothing().when(notificationService).processPcrEvent(any(PcrEventPayload.class));
        when(documentService.getDocumentIdForMaterialId(any(UUID.class), any(EntityEventType.class))).thenReturn(UUID.randomUUID());

        var response = notificationController.createNotificationPCR(payload);

        verify(notificationService, times(1)).processPcrEvent(any(PcrEventPayload.class));
        verify(documentService, times(1)).getDocumentIdForMaterialId(any(UUID.class), any(EntityEventType.class));
        verify(callbackDeliveryService, times(1)).processPcrEvent(any(PcrEventPayload.class), any(UUID.class));
        assert response.getStatusCode().value() == 202;
        assert response.getBody() == null;
    }

    @SneakyThrows
    @Test
    void runtime_exception_should_not_resolve_subscribers() {
        PcrEventPayload payload = PcrEventPayload.builder().materialId(UUID.randomUUID()).build();

        doThrow(new RuntimeException("processing failed"))
                .when(notificationService)
                .processPcrEvent(any(PcrEventPayload.class));

        assertThrows(RuntimeException.class, () -> notificationController.createNotificationPCR(payload));
        verify(notificationService, times(1)).processPcrEvent(any(PcrEventPayload.class));
        verify(documentService, never()).getDocumentIdForMaterialId(any(), any());
    }

    @SneakyThrows
    @Test
    void illegal_state_should_not_resolve_subscribers() {
        PcrEventPayload payload = PcrEventPayload.builder().materialId(UUID.randomUUID()).build();

        doThrow(new IllegalStateException("Service is in an invalid state"))
                .when(notificationService)
                .processPcrEvent(any(PcrEventPayload.class));

        assertThrows(IllegalStateException.class, () -> notificationController.createNotificationPCR(payload));
        verify(notificationService, times(1)).processPcrEvent(any(PcrEventPayload.class));
        verify(documentService, never()).getDocumentIdForMaterialId(any(), any());
    }

    @SneakyThrows
    @Test
    void unexpected_exception_should_not_resolve_subscribers() {
        PcrEventPayload payload = PcrEventPayload.builder().materialId(UUID.randomUUID()).build();

        doThrow(new RuntimeException("Unexpected error occurred", new Exception("Root cause")))
                .when(notificationService)
                .processPcrEvent(any(PcrEventPayload.class));

        assertThrows(RuntimeException.class, () -> notificationController.createNotificationPCR(payload));
        verify(notificationService, times(1)).processPcrEvent(any(PcrEventPayload.class));
        verify(documentService, never()).getDocumentIdForMaterialId(any(), any());
    }

    @SneakyThrows
    @Test
    void null_pointer_should_not_resolve_subscribers() {
        PcrEventPayload payload = PcrEventPayload.builder().materialId(UUID.randomUUID()).build();

        doThrow(new NullPointerException("Null value encountered"))
                .when(notificationService)
                .processPcrEvent(any(PcrEventPayload.class));

        assertThrows(NullPointerException.class, () -> notificationController.createNotificationPCR(payload));
        verify(notificationService, times(1)).processPcrEvent(any(PcrEventPayload.class));
        verify(documentService, never()).getDocumentIdForMaterialId(any(), any());
    }
}
