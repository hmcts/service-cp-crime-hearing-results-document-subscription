package uk.gov.hmcts.cp.subscription.unit.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import uk.gov.hmcts.cp.openapi.model.EventType;
import uk.gov.hmcts.cp.openapi.model.PcrEventPayload;
import uk.gov.hmcts.cp.subscription.controllers.NotificationController;
import uk.gov.hmcts.cp.subscription.managers.NotificationManager;
import uk.gov.hmcts.cp.subscription.model.DocumentContent;
import uk.gov.hmcts.cp.subscription.services.exceptions.CallbackUrlDeliveryException;

import java.net.URISyntaxException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.ACCEPTED;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    private static final UUID MATERIAL_ID = UUID.randomUUID();
    private static final UUID DOCUMENT_ID = UUID.randomUUID();
    private static final UUID SUBSCRIPTION_ID = UUID.randomUUID();

    @Mock
    NotificationManager notificationManager;

    @InjectMocks
    NotificationController notificationController;

    @SneakyThrows
    @Test
    void valid_pcr_payload_should_process_deliver_callbackurl_and_return_accepted() {
        PcrEventPayload payload = PcrEventPayload.builder()
                .materialId(MATERIAL_ID)
                .eventType(EventType.PRISON_COURT_REGISTER_GENERATED)
                .build();
        doNothing().when(notificationManager).processPcrNotification(any(PcrEventPayload.class));

        var response = notificationController.createNotificationPCR(payload);

        verify(notificationManager).processPcrNotification(eq(payload));
        assertThat(response.getStatusCode()).isEqualTo(ACCEPTED);
    }

    @SneakyThrows
    @Test
    void runtime_exception_should_propagate() {
        PcrEventPayload payload = PcrEventPayload.builder().materialId(MATERIAL_ID).build();

        doThrow(new RuntimeException("processing failed"))
                .when(notificationManager)
                .processPcrNotification(any(PcrEventPayload.class));

        assertThrows(RuntimeException.class, () -> notificationController.createNotificationPCR(payload));
        verify(notificationManager).processPcrNotification(eq(payload));
    }

    @SneakyThrows
    @Test
    void json_processing_exception_in_callback_should_throw_callback_url_delivery_exception() {
        PcrEventPayload payload = PcrEventPayload.builder()
                .materialId(MATERIAL_ID)
                .eventType(EventType.PRISON_COURT_REGISTER_GENERATED)
                .build();
        JsonProcessingException cause = new JsonProcessingException("Invalid JSON") {};

        doThrow(new CallbackUrlDeliveryException("PCR - Failed to build or deliver callback payload: " + cause.getMessage(), cause))
                .when(notificationManager).processPcrNotification(any(PcrEventPayload.class));

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

        doThrow(new CallbackUrlDeliveryException("PCR - Failed to build or deliver callback payload: " + cause.getMessage(), cause))
                .when(notificationManager).processPcrNotification(any(PcrEventPayload.class));

        CallbackUrlDeliveryException thrown = assertThrows(CallbackUrlDeliveryException.class,
                () -> notificationController.createNotificationPCR(payload));

        assertThat(thrown.getMessage()).contains("PCR - Failed to build or deliver callback payload");
        assertThat(thrown.getCause()).isEqualTo(cause);
    }

    @Test
    void get_pcr_document_should_return_200_with_content_from_manager() throws Exception {
        byte[] pdfBody = "PDF content".getBytes();
        DocumentContent content = DocumentContent.builder()
                .body(pdfBody)
                .contentType(MediaType.APPLICATION_PDF)
                .fileName("PrisonCourtRegister.pdf")
                .build();
        when(notificationManager.getPcrDocumentContent(eq(SUBSCRIPTION_ID), eq(DOCUMENT_ID))).thenReturn(content);

        var response = notificationController.getPcrDocumentByClientSubscription(SUBSCRIPTION_ID, DOCUMENT_ID);

        assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getInputStream().readAllBytes()).isEqualTo(pdfBody);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("PrisonCourtRegister.pdf");
        verify(notificationManager).getPcrDocumentContent(SUBSCRIPTION_ID, DOCUMENT_ID);
    }
}
