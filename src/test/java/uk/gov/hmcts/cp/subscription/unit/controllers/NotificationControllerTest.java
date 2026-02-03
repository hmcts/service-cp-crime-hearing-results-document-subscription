package uk.gov.hmcts.cp.subscription.unit.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.hmcts.cp.openapi.model.EventType;
import uk.gov.hmcts.cp.openapi.model.PcrEventPayload;
import uk.gov.hmcts.cp.subscription.controllers.NotificationController;
import uk.gov.hmcts.cp.subscription.model.EntityEventType;
import uk.gov.hmcts.cp.subscription.services.CallbackDeliveryService;
import uk.gov.hmcts.cp.subscription.services.DocumentService;
import uk.gov.hmcts.cp.subscription.services.NotificationService;
import uk.gov.hmcts.cp.subscription.services.exceptions.CallbackUrlDeliveryException;

import java.net.URISyntaxException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.ACCEPTED;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    private static final UUID MATERIAL_ID = UUID.randomUUID();
    private static final UUID DOCUMENT_ID = UUID.randomUUID();

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
                .materialId(MATERIAL_ID)
                .eventType(EventType.PRISON_COURT_REGISTER_GENERATED)
                .build();
        doNothing().when(notificationService).processInboundEvent(any(PcrEventPayload.class));
        when(documentService.getDocumentIdForMaterialId(any(UUID.class), any(EntityEventType.class))).thenReturn(DOCUMENT_ID);

        var response = notificationController.createNotificationPCR(payload);

        verify(notificationService).processInboundEvent(any(PcrEventPayload.class));
        verify(documentService).getDocumentIdForMaterialId(any(UUID.class), any(EntityEventType.class));
        verify(callbackDeliveryService).processPcrEvent(any(PcrEventPayload.class), any(UUID.class));
        assertThat(response.getStatusCode()).isEqualTo(ACCEPTED);
    }

    @SneakyThrows
    @Test
    void runtime_exception_should_not_resolve_subscribers() {
        PcrEventPayload payload = PcrEventPayload.builder().materialId(MATERIAL_ID).build();

        doThrow(new RuntimeException("processing failed"))
                .when(notificationService)
                .processInboundEvent(any(PcrEventPayload.class));

        assertThrows(RuntimeException.class, () -> notificationController.createNotificationPCR(payload));
        verify(notificationService).processInboundEvent(any(PcrEventPayload.class));
        verify(documentService, never()).getDocumentIdForMaterialId(any(), any());
    }

    @SneakyThrows
    @Test
    void json_processing_exception_in_callback_should_throw_callback_url_delivery_exception() {
        PcrEventPayload payload = PcrEventPayload.builder()
                .materialId(MATERIAL_ID)
                .eventType(EventType.PRISON_COURT_REGISTER_GENERATED)
                .build();
        JsonProcessingException cause = new JsonProcessingException("Invalid JSON") {};

        doNothing().when(notificationService).processInboundEvent(any(PcrEventPayload.class));
        when(documentService.getDocumentIdForMaterialId(any(UUID.class), any(EntityEventType.class))).thenReturn(DOCUMENT_ID);
        doThrow(cause).when(callbackDeliveryService).processPcrEvent(any(PcrEventPayload.class), any(UUID.class));

        CallbackUrlDeliveryException thrown = assertThrows(CallbackUrlDeliveryException.class,
                () -> notificationController.createNotificationPCR(payload));

        assertThat(thrown.getMessage()).contains("PCR - Failed to build or deliver callback payload");
        assertThat(thrown.getCause()).isEqualTo(cause);
    }

    @SneakyThrows
    @Test
    void uri_syntax_exception_in_callback_should_throw_callback_url_delivery_exception() {
        PcrEventPayload payload = PcrEventPayload.builder()
                .materialId(MATERIAL_ID)
                .eventType(EventType.PRISON_COURT_REGISTER_GENERATED)
                .build();
        URISyntaxException cause = new URISyntaxException("invalid", "bad uri");

        doNothing().when(notificationService).processInboundEvent(any(PcrEventPayload.class));
        when(documentService.getDocumentIdForMaterialId(any(UUID.class), any(EntityEventType.class))).thenReturn(DOCUMENT_ID);
        doThrow(cause).when(callbackDeliveryService).processPcrEvent(any(PcrEventPayload.class), any(UUID.class));

        CallbackUrlDeliveryException thrown = assertThrows(CallbackUrlDeliveryException.class,
                () -> notificationController.createNotificationPCR(payload));

        assertThat(thrown.getMessage()).contains("PCR - Failed to build or deliver callback payload");
        assertThat(thrown.getCause()).isEqualTo(cause);
    }
}
