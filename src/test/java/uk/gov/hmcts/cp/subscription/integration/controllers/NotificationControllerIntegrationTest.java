package uk.gov.hmcts.cp.subscription.integration.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

import java.util.List;
import java.util.UUID;

import static java.util.UUID.randomUUID;
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


@EnableWireMock({@ConfigureWireMock(name = "material-client", baseUrlProperties = "material-client.url", port = 18081)})
class NotificationControllerIntegrationTest extends IntegrationTestBase {

    private static final String NOTIFICATION_PCR_URI = "/notifications/pcr";

    /** Material ID stubbed to return 200 with metadata (material-metadata-mapping.json). */
    private static final UUID MATERIAL_ID = UUID.fromString("6c198796-08bb-4803-b456-fa0c29ca6021");

    @MockitoBean
    private CallbackDeliveryService callbackDeliveryService;

    @BeforeEach
    void setUp() {
        reset(callbackDeliveryService);
        clearClientSubscriptionTable();
        clearDocumentMappingTable();
    }

    @Test
    void prison_court_register_generated_should_return_success() throws Exception {
        String pcrPayload = createPcrPayload(randomUUID(), MATERIAL_ID, "PRISON_COURT_REGISTER_GENERATED");

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
        UUID eventId = randomUUID();
        String pcrPayload = createPcrPayload(eventId, MATERIAL_ID, "CUSTODIAL_RESULT");

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
    void get_document_should_return_200_with_pdf_when_subscription_has_access() throws Exception {
        ClientSubscriptionEntity subscription = insertSubscription(
                "https://callback.example.com",
                List.of(EntityEventType.PRISON_COURT_REGISTER_GENERATED));
        DocumentMappingEntity document = insertDocument(MATERIAL_ID, EntityEventType.PRISON_COURT_REGISTER_GENERATED);

        mockMvc.perform(get("/client-subscriptions/{clientSubscriptionId}/documents/{documentId}",
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
                "https://callback.example.com",
                List.of(EntityEventType.CUSTODIAL_RESULT));
        DocumentMappingEntity document = insertDocument(MATERIAL_ID, EntityEventType.PRISON_COURT_REGISTER_GENERATED);

        mockMvc.perform(get("/client-subscriptions/{clientSubscriptionId}/documents/{documentId}",
                        subscription.getId(), document.getDocumentId()))
                .andDo(print())
                .andExpect(status().isForbidden());
    }
}
