package uk.gov.hmcts.cp.subscription.integration.controllers;

import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.cp.subscription.controllers.GlobalExceptionHandler;
import uk.gov.hmcts.cp.subscription.controllers.NotificationController;
import uk.gov.hmcts.cp.subscription.model.DocumentContent;
import uk.gov.hmcts.cp.subscription.services.DocumentService;
import uk.gov.hmcts.cp.subscription.services.NotificationService;
import uk.gov.hmcts.cp.subscription.services.CallbackDeliveryService;

import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

/**
 * Validation tests for PCR notification API. Uses @WebMvcTest only (no Docker, DB, or WireMock).
 */
@WebMvcTest(NotificationController.class)
@Import(GlobalExceptionHandler.class)
class NotificationControllerValidationTest {

    private static final String NOTIFICATION_PCR_URI = "/notifications/pcr";
    private static final String PCR_REQUEST_TEMPLATE = "stubs/requests/pcr-request.json";
    private static final UUID MATERIAL_ID = randomUUID();
    private static final UUID EVENT_ID = randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private DocumentService documentService;

    @MockitoBean
    private CallbackDeliveryService callbackDeliveryService;

    @Test
    void bad_content_type_should_return_415() throws Exception {
        String pcrPayload = createPcrPayload(EVENT_ID, MATERIAL_ID);

        mockMvc.perform(post(NOTIFICATION_PCR_URI)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(pcrPayload))
                .andDo(print())
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void invalid_payload_missing_materialid_should_return_400() throws Exception {
        String pcrPayload = createPcrPayload(EVENT_ID, null);

        mockMvc.perform(post(NOTIFICATION_PCR_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pcrPayload))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalid_payload_missing_eventid_should_return_400() throws Exception {
        String pcrPayload = createPcrPayload(null, MATERIAL_ID);

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
    void material_metadata_not_found_should_return_404() throws Exception {
        UUID materialId = UUID.fromString("6c198796-08bb-4803-b456-fa0c29ca6022");
        String pcrPayload = createPcrPayload(randomUUID(), materialId);

        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Material not found"))
                .when(notificationService).processPcrEvent(any());

        mockMvc.perform(post(NOTIFICATION_PCR_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pcrPayload))
                .andDo(print())
                .andExpect(status().isNotFound());
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
        UUID subscriptionId = randomUUID();
        UUID documentId = randomUUID();
        byte[] pdfContent = "PDF content".getBytes();
        DocumentContent documentContent = DocumentContent.builder()
                .body(pdfContent)
                .contentType("application/pdf")
                .fileName("PrisonCourtRegister.pdf")
                .build();

        doReturn(documentContent).when(documentService)
                .getDocumentContentAsBinary(eq(subscriptionId), eq(documentId));

        mockMvc.perform(get("/client-subscriptions/{clientSubscriptionId}/documents/{documentId}",
                        subscriptionId, documentId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"PrisonCourtRegister.pdf\""))
                .andExpect(content().bytes(pdfContent));
    }

    @Test
    void get_document_should_return_403_when_subscription_does_not_have_access() throws Exception {
        UUID subscriptionId = randomUUID();
        UUID documentId = randomUUID();

        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: subscription does not have access to this document"))
                .when(documentService).getDocumentContentAsBinary(eq(subscriptionId), eq(documentId));

        mockMvc.perform(get("/client-subscriptions/{clientSubscriptionId}/documents/{documentId}",
                        subscriptionId, documentId))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(content().string("Access denied: subscription does not have access to this document"));
    }

    @Test
    void get_document_should_return_400_when_invalid_subscription_uuid() throws Exception {
        mockMvc.perform(get("/client-subscriptions/{clientSubscriptionId}/documents/{documentId}",
                        "invalid-uuid", randomUUID()))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void get_document_should_return_400_when_invalid_document_uuid() throws Exception {
        mockMvc.perform(get("/client-subscriptions/{clientSubscriptionId}/documents/{documentId}",
                        randomUUID(), "invalid-uuid"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void get_document_should_return_400_when_document_not_found() throws Exception {
        UUID subscriptionId = randomUUID();
        UUID documentId = randomUUID();

        doThrow(new java.util.NoSuchElementException("Document not found"))
                .when(documentService).getDocumentContentAsBinary(eq(subscriptionId), eq(documentId));

        mockMvc.perform(get("/client-subscriptions/{clientSubscriptionId}/documents/{documentId}",
                        subscriptionId, documentId))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void callbackUrl_delivery_failure_should_return_502() throws Exception {
        String pcrPayload = createPcrPayload(EVENT_ID, MATERIAL_ID);

        // return any documentId so controller proceeds to callbackUrl delivery
        org.mockito.Mockito.doReturn(randomUUID())
                .when(documentService).getDocumentIdForMaterialId(any(), any());

        // callbackUrl delivery failing with 502
        doThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "CallbackUrl delivery failed"))
                .when(callbackDeliveryService).processPcrEvent(any(), any());

        mockMvc.perform(post(NOTIFICATION_PCR_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pcrPayload))
                .andDo(print())
                .andExpect(status().isBadGateway());
    }

    private static String createPcrPayload(UUID eventId, UUID materialId) throws IOException {
        return createPcrPayload(eventId, materialId, "PRISON_COURT_REGISTER_GENERATED");
    }

    private static String createPcrPayload(UUID eventId, UUID materialId, String eventType) throws IOException {
        ClassPathResource resource = new ClassPathResource(PCR_REQUEST_TEMPLATE);
        String template = Files.readString(resource.getFile().toPath());
        template = template.replaceAll("EVENT_TYPE", eventType != null ? eventType : "PRISON_COURT_REGISTER_GENERATED");
        if (nonNull(eventId)) {
            template = template.replaceAll("EVENT_ID", eventId.toString());
        } else {
            template = template.replaceAll("EVENT_ID", "null");
        }
        if (nonNull(materialId)) {
            template = template.replaceAll("MATERIAL_ID", materialId.toString());
        } else {
            template = template.replaceAll("MATERIAL_ID", "null");
        }
        return template;
    }
}
