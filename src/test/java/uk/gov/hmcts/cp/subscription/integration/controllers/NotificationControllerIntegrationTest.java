package uk.gov.hmcts.cp.subscription.integration.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import uk.gov.hmcts.cp.openapi.model.PcrEventPayload;
import uk.gov.hmcts.cp.subscription.entities.ClientSubscriptionEntity;
import uk.gov.hmcts.cp.subscription.entities.DocumentMappingEntity;
import uk.gov.hmcts.cp.subscription.integration.IntegrationTestBase;
import uk.gov.hmcts.cp.subscription.model.EntityEventType;
import uk.gov.hmcts.cp.subscription.services.CallbackDeliveryService;


@EnableWireMock({@ConfigureWireMock(name = "material-client", baseUrlProperties = "material-client.url", port = 0)})
@ActiveProfiles("test")
class NotificationControllerIntegrationTest extends IntegrationTestBase {

    private static final String NOTIFICATION_PCR_URI = "/notifications/pcr";
    private static final String CALLBACK_URL = "https://callback.example.com";
    private static final UUID MATERIAL_ID = UUID.fromString("6c198796-08bb-4803-b456-fa0c29ca6021");
    private static final String DOCUMENT_URI = "/client-subscriptions/{clientSubscriptionId}/documents/{documentId}";

    @MockitoBean
    private CallbackDeliveryService callbackDeliveryService;

    @BeforeEach
    void setUp() {
        reset(callbackDeliveryService);
        clearAllTables();
    }

    @Test
    void prison_court_register_generated_should_return_success() throws Exception {
        String pcrPayload = loadPayload("stubs/requests/progression/pcr-request-prison-court-register.json");

        mockMvc.perform(post(NOTIFICATION_PCR_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                        .content(pcrPayload))
                .andDo(print())
                .andExpect(status().isAccepted());

        verify(callbackDeliveryService, times(1)).processPcrEvent(any(PcrEventPayload.class), any(UUID.class));
    }

    @Test
    void custodial_result_should_return_unsupported() throws Exception {
        String pcrPayload = loadPayload("stubs/requests/progression/pcr-request-custodial-result.json");

        doThrow(new UnsupportedOperationException("CUSTODIAL_RESULT not implemented"))
                .when(callbackDeliveryService).processPcrEvent(any(PcrEventPayload.class), any(UUID.class));

        mockMvc.perform(post(NOTIFICATION_PCR_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                        .content(pcrPayload))
                .andDo(print())
                .andExpect(status().isNotImplemented())
                .andExpect(content().string("Unsupported"));

        verify(callbackDeliveryService, times(1)).processPcrEvent(any(PcrEventPayload.class), any(UUID.class));
    }

    @Test
    void material_metadata_not_found_should_return_404() throws Exception {
        String pcrPayload = loadPayload("stubs/requests/progression/pcr-request-material-not-found.json");

        mockMvc.perform(post(NOTIFICATION_PCR_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                        .content(pcrPayload))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    void material_metadata_timeout_should_return_504_via_global_exception_handler() throws Exception {
        String pcrPayload = loadPayload("stubs/requests/progression/pcr-request-material-timeout.json");


        mockMvc.perform(post(NOTIFICATION_PCR_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                        .content(pcrPayload))
                .andDo(print())
                .andExpect(status().isGatewayTimeout())
                .andExpect(content().string("Material metadata not ready"));
    }

    @Test
    void get_document_should_return_200_with_pdf_when_subscription_has_access() throws Exception {
        ClientSubscriptionEntity subscription = insertSubscription(
                CALLBACK_URL,
                List.of(EntityEventType.PRISON_COURT_REGISTER_GENERATED));
        DocumentMappingEntity document = insertDocument(MATERIAL_ID, EntityEventType.PRISON_COURT_REGISTER_GENERATED);

        mockMvc.perform(get(DOCUMENT_URI,
                        subscription.getId(), document.getDocumentId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"PrisonCourtRegister_20251219083322.pdf\""))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void get_document_should_return_403_when_subscription_does_not_have_event_type() throws Exception {
        ClientSubscriptionEntity subscription = insertSubscription(
                CALLBACK_URL,
                List.of(EntityEventType.CUSTODIAL_RESULT));
        DocumentMappingEntity document = insertDocument(MATERIAL_ID, EntityEventType.PRISON_COURT_REGISTER_GENERATED);

        mockMvc.perform(get(DOCUMENT_URI,
                        subscription.getId(), document.getDocumentId()))
                .andDo(print())
                .andExpect(status().isForbidden());
    }
}
