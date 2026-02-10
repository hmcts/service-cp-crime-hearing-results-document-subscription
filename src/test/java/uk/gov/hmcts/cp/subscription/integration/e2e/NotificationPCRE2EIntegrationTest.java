package uk.gov.hmcts.cp.subscription.integration.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

import uk.gov.hmcts.cp.material.openapi.api.MaterialApi;
import uk.gov.hmcts.cp.openapi.model.PcrEventPayload;
import uk.gov.hmcts.cp.subscription.integration.IntegrationTestBase;
import uk.gov.hmcts.cp.subscription.model.EntityEventType;
import uk.gov.hmcts.cp.subscription.services.CallbackDeliveryService;
import uk.gov.hmcts.cp.subscription.services.DocumentService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// TODO - comments can be removed after handover to QA

@EnableWireMock({@ConfigureWireMock(name = "material-client", baseUrlProperties = "material-client.url", port = 0)})
@TestPropertySource(properties = {
        "material-client.retry.timeoutMilliSecs=500",
        "material-client.retry.intervalMilliSecs=100"
})
class NotificationPcrE2EIntegrationTest extends IntegrationTestBase {

    private static final String NOTIFICATIONS_PCR_URI = "/notifications/pcr";
    private static final String CLIENT_SUBSCRIPTIONS_URI = "/client-subscriptions";
    private static final String DOCUMENT_URI = CLIENT_SUBSCRIPTIONS_URI + "/{clientSubscriptionId}/documents/{documentId}";
    private static final String SUBSCRIPTION_REQUEST_E2E = "stubs/requests/subscription/subscription-pcr-request.json";
    private static final String PCR_EVENT_PAYLOAD_PATH = "stubs/requests/progression-pcr/pcr-request-prison-court-register.json";
    private static final String PCR_EVENT_TIMEOUT_PATH = "stubs/requests/progression-pcr/pcr-request-material-timeout.json";
    private static final UUID MATERIAL_ID = UUID.fromString("6c198796-08bb-4803-b456-fa0c29ca6021");
    private static final UUID MATERIAL_ID_TIMEOUT = UUID.fromString("11111111-1111-1111-1111-111111111112");

    @MockitoBean
    private CallbackDeliveryService callbackDeliveryService;

    @MockitoSpyBean
    private MaterialApi materialApi;

    @MockitoSpyBean
    private DocumentService documentService;

    private UUID subscriptionId;
    private UUID callbackDocumentId;

    @BeforeEach
    void setUp() {
        reset(callbackDeliveryService);
        clearAllTables();
    }

    @Test
    void test_document_retrieval_success() throws Exception {
        given_i_am_a_subscriber_with_a_subscription();
        given_i_have_a_callback_endpoint();
        given_material_service_returns_document_success();

        when_a_pcr_event_is_posted();
        when_material_service_responds();

        then_the_material_api_is_called();
        then_the_subscriber_receives_a_callback();
        then_the_subscriber_can_retrieve_the_document();
    }

    @Test
    void test_document_retrieval_failure() throws Exception {
        given_i_am_a_subscriber_with_a_subscription();
        given_i_have_a_callback_endpoint();
        given_material_service_returns_document_not_found();

        when_a_pcr_event_is_posted_with_timeout();
        when_material_service_responds();

        then_the_material_api_was_polled();
        then_the_subscriber_does_not_receive_a_callback();
        then_the_subscriber_cannot_retrieve_the_document();
    }

    private void given_i_am_a_subscriber_with_a_subscription() throws Exception {
        createSubscription();
    }

    private void given_i_have_a_callback_endpoint() {
        // Callback endpoint represented by @MockitoBean CallbackDeliveryService
    }

    private void given_material_service_returns_document_success() {
         /*WireMock (material-client) stubs from wiremock/material-client/mappings/:
         material-metadata-mapping.json     GET .../material/6c198796-08bb-4803-b456-fa0c29ca6021/metadata  → 200, __files/material-response.json
         material-content-full-mapping.json GET .../material/6c198796-08bb-4803-b456-fa0c29ca6021/content   → 200, __files/material-with-contenturl.json
         material-binary-mapping.json       GET .../material/6c198796-08bb-4803-b456-fa0c29ca6021/binary    → 200, __files/material-content.pdf*/
    }

    private void given_material_service_returns_document_not_found() {
         /*Request payload: stubs/requests/progression-pcr/pcr-request-material-timeout.json
           eventId: 11111111-1111-1111-1111-111111111111, materialId: 11111111-1111-1111-1111-111111111112, eventType: PRISON_COURT_REGISTER_GENERATED
           WireMock stub (wiremock/material-client/mappings/):
           material-metadata-timeout-mapping.json  GET .../material/11111111-1111-1111-1111-111111111112/metadata  → 204 (no body)
           App polls until Awaitility times out → 504 "Material metadata not ready", no save/callback.*/
    }

    private void when_a_pcr_event_is_posted() throws Exception {
        mockMvc.perform(post(NOTIFICATIONS_PCR_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                        .content(loadPayload(PCR_EVENT_PAYLOAD_PATH)))
                .andDo(print())
                .andExpect(status().isAccepted());
    }

    private void when_a_pcr_event_is_posted_with_timeout() throws Exception {
        mockMvc.perform(post(NOTIFICATIONS_PCR_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                        .content(loadPayload(PCR_EVENT_TIMEOUT_PATH)))
                .andDo(print())
                .andExpect(status().isGatewayTimeout())
                .andExpect(content().string("Material metadata not ready"));
    }

    private void when_material_service_responds() {
        /* This step does nothing; the material response already happens during when_a_pcr_event_is_posted (app calls MaterialApi, WireMock serves stubs)
           Success: GET metadata → 200 → save document mapping → callback.
           Failure: GET metadata → 204 until timeout → 504.*/
    }

    private void then_the_material_api_is_called() {
        verify(materialApi, times(1)).getMaterialMetadataByMaterialId(eq(MATERIAL_ID));
    }

    private void then_the_material_api_was_polled() {
        verify(materialApi, atLeastOnce()).getMaterialMetadataByMaterialId(eq(MATERIAL_ID_TIMEOUT));
    }

    private void then_the_subscriber_receives_a_callback() {
        ArgumentCaptor<UUID> documentIdCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(callbackDeliveryService, times(1))
                .processPcrEvent(any(PcrEventPayload.class), documentIdCaptor.capture());
        callbackDocumentId = documentIdCaptor.getValue();
        assertThat(callbackDocumentId).isNotNull();
    }

    private void then_the_subscriber_does_not_receive_a_callback() {
        verify(callbackDeliveryService, times(0)).processPcrEvent(any(PcrEventPayload.class), any());
    }

    private void then_the_subscriber_can_retrieve_the_document() throws Exception {
        mockMvc.perform(get(DOCUMENT_URI, subscriptionId, callbackDocumentId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("application/pdf")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("PrisonCourtRegister")))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    private void then_the_subscriber_cannot_retrieve_the_document() {
        verify(documentService, times(0)).saveDocumentMapping(any(UUID.class), any(EntityEventType.class));
    }

    private void createSubscription() throws Exception {
        String json = mockMvc.perform(post(CLIENT_SUBSCRIPTIONS_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loadPayload(SUBSCRIPTION_REQUEST_E2E)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientSubscriptionId").exists())
                .andReturn().getResponse().getContentAsString();
        subscriptionId = UUID.fromString(new ObjectMapper().readTree(json).get("clientSubscriptionId").asText());
    }
}
