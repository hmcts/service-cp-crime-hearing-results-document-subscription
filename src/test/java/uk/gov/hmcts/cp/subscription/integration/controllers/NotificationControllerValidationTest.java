package uk.gov.hmcts.cp.subscription.integration.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.cp.subscription.controllers.GlobalExceptionHandler;
import uk.gov.hmcts.cp.subscription.controllers.NotificationController;
import uk.gov.hmcts.cp.subscription.managers.NotificationManager;
import uk.gov.hmcts.cp.subscription.model.DocumentContent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@Import(GlobalExceptionHandler.class)
class NotificationControllerValidationTest {

    private static final String NOTIFICATION_PCR_URI = "/notifications/pcr";
    private static final String PCR_REQUEST_VALID = "stubs/requests/pcr-request-valid.json";
    private static final String PCR_REQUEST_MISSING_MATERIAL = "stubs/requests/pcr-request-missing-material.json";
    private static final String PCR_REQUEST_MISSING_EVENT = "stubs/requests/pcr-request-missing-event.json";
    private static final UUID SUBSCRIPTION_ID = randomUUID();
    private static final UUID DOCUMENT_ID = randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationManager notificationManager;

    @Test
    void bad_content_type_should_return_415() throws Exception {
        String pcrPayload = loadPcrPayload(PCR_REQUEST_VALID);

        mockMvc.perform(post(NOTIFICATION_PCR_URI)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(pcrPayload))
                .andDo(print())
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void invalid_payload_missing_materialid_should_return_400() throws Exception {
        String pcrPayload = loadPcrPayload(PCR_REQUEST_MISSING_MATERIAL);

        mockMvc.perform(post(NOTIFICATION_PCR_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pcrPayload))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalid_payload_missing_eventid_should_return_400() throws Exception {
        String pcrPayload = loadPcrPayload(PCR_REQUEST_MISSING_EVENT);

        mockMvc.perform(post(NOTIFICATION_PCR_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pcrPayload))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void malformed_payload_should_return_400() throws Exception {
        String malformedJson = "{ \"eventId\": \"invalid-uuid\", \"materialId\": }";

        mockMvc.perform(post(NOTIFICATION_PCR_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void empty_payload_missing_eventid_should_return_400() throws Exception {
        mockMvc.perform(post(NOTIFICATION_PCR_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void missing_body_should_return_400() throws Exception {
        mockMvc.perform(post(NOTIFICATION_PCR_URI)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void get_document_should_return_200_with_pdf_content_when_subscription_has_access() throws Exception {
        byte[] pdfContent = "PDF content".getBytes();
        DocumentContent documentContent = DocumentContent.builder()
                .body(pdfContent)
                .contentType(MediaType.APPLICATION_PDF)
                .fileName("PrisonCourtRegister.pdf")
                .build();

        when(notificationManager.getPcrDocumentContent(eq(SUBSCRIPTION_ID), eq(DOCUMENT_ID))).thenReturn(documentContent);

        mockMvc.perform(get("/client-subscriptions/{clientSubscriptionId}/documents/{documentId}",
                        SUBSCRIPTION_ID, DOCUMENT_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"PrisonCourtRegister.pdf\""))
                .andExpect(content().bytes(pdfContent));
    }

    @Test
    void get_document_should_return_403_when_subscription_does_not_have_access() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: subscription does not have access to this document"))
                .when(notificationManager).getPcrDocumentContent(eq(SUBSCRIPTION_ID), eq(DOCUMENT_ID));

        mockMvc.perform(get("/client-subscriptions/{clientSubscriptionId}/documents/{documentId}",
                        SUBSCRIPTION_ID, DOCUMENT_ID))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(content().string("Access denied: subscription does not have access to this document"));
    }

    @Test
    void get_document_should_return_400_when_invalid_subscription_uuid() throws Exception {
        mockMvc.perform(get("/client-subscriptions/{clientSubscriptionId}/documents/{documentId}",
                        "invalid-uuid", DOCUMENT_ID))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void get_document_should_return_400_when_invalid_document_uuid() throws Exception {
        mockMvc.perform(get("/client-subscriptions/{clientSubscriptionId}/documents/{documentId}",
                        SUBSCRIPTION_ID, "invalid-uuid"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void get_document_should_return_404_when_document_not_found() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found: " + DOCUMENT_ID))
                .when(notificationManager).getPcrDocumentContent(eq(SUBSCRIPTION_ID), eq(DOCUMENT_ID));

        mockMvc.perform(get("/client-subscriptions/{clientSubscriptionId}/documents/{documentId}",
                        SUBSCRIPTION_ID, DOCUMENT_ID))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    void callbackUrl_delivery_failure_should_return_502() throws Exception {
        String pcrPayload = loadPcrPayload(PCR_REQUEST_VALID);

        doThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "CallbackUrl delivery failed"))
                .when(notificationManager).processPcrNotification(any());

        mockMvc.perform(post(NOTIFICATION_PCR_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pcrPayload))
                .andDo(print())
                .andExpect(status().isBadGateway());
    }

    private String loadPcrPayload(String path) throws IOException {
        return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
    }
}
