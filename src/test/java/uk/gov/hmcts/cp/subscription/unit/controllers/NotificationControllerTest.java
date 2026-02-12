package uk.gov.hmcts.cp.subscription.unit.controllers;

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
    void exception_from_manager_should_propagate_for_global_handler() {
        PcrEventPayload payload = PcrEventPayload.builder()
                .materialId(MATERIAL_ID)
                .eventType(EventType.PRISON_COURT_REGISTER_GENERATED)
                .build();
        RuntimeException failure = new RuntimeException("Callback delivery failed");

        doThrow(failure)
                .when(notificationManager).processPcrNotification(any(PcrEventPayload.class));

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> notificationController.createNotificationPCR(payload));

        assertThat(thrown).isSameAs(failure);
        verify(notificationManager).processPcrNotification(eq(payload));
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

        var response = notificationController.getDocument(SUBSCRIPTION_ID, DOCUMENT_ID);

        assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getInputStream().readAllBytes()).isEqualTo(pdfBody);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("PrisonCourtRegister.pdf");
        verify(notificationManager).getPcrDocumentContent(SUBSCRIPTION_ID, DOCUMENT_ID);
    }
}
