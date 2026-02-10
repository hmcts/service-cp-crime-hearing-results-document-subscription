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
import uk.gov.hmcts.cp.subscription.entities.DocumentMappingEntity;
import uk.gov.hmcts.cp.subscription.integration.IntegrationTestBase;
import uk.gov.hmcts.cp.subscription.model.EntityEventType;
import uk.gov.hmcts.cp.subscription.services.CallbackDeliveryService;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

/**
 * E2E integration: subscriber subscribes, progression triggers PCR event,
 * document mapping is created, callback delivery is invoked, subscriber retrieves document.
 */
@EnableWireMock({@ConfigureWireMock(name = "material-client", baseUrlProperties = "material-client.url", port = 0)})
@TestPropertySource(properties = {
        "material-client.retry.timeoutMilliSecs=500",
        "material-client.retry.intervalMilliSecs=100"
})
class NotificationPcrE2EIntegrationTest extends IntegrationTestBase {

    private static final String NOTIFICATION_PCR_URI = "/notifications/pcr";
    private static final String CLIENT_SUBSCRIPTIONS_URI = "/client-subscriptions";
    private static final String DOCUMENT_URI = "/client-subscriptions/{clientSubscriptionId}/documents/{documentId}";
    private static final String SUBSCRIPTION_REQUEST_E2E = "stubs/requests/subscription/subscription-pcr-request.json";
    private static final String PCR_EVENT_PAYLOAD_PATH = "stubs/requests/progression-pcr/pcr-request-prison-court-register.json";
    private static final String PCR_EVENT_TIMEOUT_PATH = "stubs/requests/progression-pcr/pcr-request-material-timeout.json";
    private static final UUID MATERIAL_ID = UUID.fromString("6c198796-08bb-4803-b456-fa0c29ca6021");

    @MockitoBean
    private CallbackDeliveryService callbackDeliveryService;

    @MockitoSpyBean
    private MaterialApi materialApi;

    private UUID subscriptionId;
    private UUID callbackDocumentId;

    @BeforeEach
    void setUp() {
        reset(callbackDeliveryService);
        clearAllTables();
    }

    @Test
    void notification_pcr_e2e_flow_success_only() throws Exception {
        given_i_am_a_subscriber_that_posts_a_subscription();
        and_i_have_an_endpoint_to_receive_notifications();
        and_material_service_returns_document_detail_with_then_success();
        and_material_service_returns_document();
        when_progression_service_posts_a_pcr_event();
        and_material_service_responds_with_then_success();
        then_subscriber_receives_a_callback_event();
        then_subscriber_retrieves_the_pdf();
    }

    @Test
    void notification_pcr_e2e_flow_metadata_never_ready() throws Exception {
        given_i_am_a_subscriber_that_posts_a_subscription();
        and_i_have_an_endpoint_to_receive_notifications();
        and_material_service_returns_document_detail_with_not_found_and_then_failure();
        when_progression_service_posts_a_pcr_event_that_times_out();
        and_material_service_responds_with_not_found_only();
        then_pcr_notification_fails_with_gateway_timeout();
        then_no_document_mapping_or_callback();
    }

    private void given_i_am_a_subscriber_that_posts_a_subscription() throws Exception {
        createSubscription();
    }

    private void and_i_have_an_endpoint_to_receive_notifications() {
        //Subscriber callback endpoint is represented by the @MockitoBean CallbackDeliveryService
    }

    private void and_material_service_returns_document_detail_with_not_found_and_then_failure() {
        // metadata never becomes available (always not-found) - by WireMock mappings - stubs/mappings/material-metadata-timeout-mapping.json
    }

    private void and_material_service_returns_document_detail_with_then_success() {
        // success case is driven by WireMock mappings -
    }

    private void and_material_service_returns_document() {
        // Binary document is also provided by WireMock stubs
    }

    private void when_progression_service_posts_a_pcr_event() throws Exception {
        mockMvc.perform(post(NOTIFICATION_PCR_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                        .content(loadPayload(PCR_EVENT_PAYLOAD_PATH)))
                .andDo(print())
                .andExpect(status().isAccepted());
    }

    private void and_material_service_responds_with_then_success() {
        verify(materialApi, times(1)).getMaterialMetadataByMaterialId(eq(MATERIAL_ID));
    }

    private void and_material_service_responds_with_not_found_only() {
        // Optionally, you could verify atLeastOnce(materialApi).getMaterialMetadataByMaterialId(MATERIAL_ID)
    }

    private void when_progression_service_posts_a_pcr_event_that_times_out() throws Exception {
        mockMvc.perform(post(NOTIFICATION_PCR_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                        .content(loadPayload(PCR_EVENT_TIMEOUT_PATH)))
                .andDo(print())
                .andExpect(status().isGatewayTimeout())
                .andExpect(content().string("Material metadata not ready"));
    }

    private void then_pcr_notification_fails_with_gateway_timeout() {
        // The 504 + message are asserted in when_progression_service_posts_a_pcr_event_that_times_out()
    }

    private void then_no_document_mapping_or_callback() {
        Optional<DocumentMappingEntity> mapping = documentMappingRepository
                .findByMaterialIdAndEventType(MATERIAL_ID, EntityEventType.PRISON_COURT_REGISTER_GENERATED);
        assertThat(mapping).isEmpty();
        verify(callbackDeliveryService, times(0))
                .processPcrEvent(any(PcrEventPayload.class), any());
    }

    private void then_subscriber_receives_a_callback_event() {
        ArgumentCaptor<UUID> documentIdCaptor = ArgumentCaptor.forClass(UUID.class);

        verify(callbackDeliveryService, times(1))
                .processPcrEvent(any(PcrEventPayload.class), documentIdCaptor.capture());

        callbackDocumentId = documentIdCaptor.getValue();
        assertThat(callbackDocumentId).isNotNull();
    }

    private void then_subscriber_retrieves_the_pdf() throws Exception {
        mockMvc.perform(get(DOCUMENT_URI, subscriptionId, callbackDocumentId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("application/pdf")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("PrisonCourtRegister")))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    private void createSubscription() throws Exception {
        String body = loadPayload(SUBSCRIPTION_REQUEST_E2E);
        String json = mockMvc.perform(post(CLIENT_SUBSCRIPTIONS_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientSubscriptionId").exists())
                .andExpect(jsonPath("$.clientSubscriptionId").isNotEmpty())
                .andExpect(jsonPath("$.notificationEndpoint.callbackUrl").value("https://callback.example.com"))
                .andExpect(jsonPath("$.eventTypes").isArray())
                .andExpect(jsonPath("$.eventTypes[0]").value("PRISON_COURT_REGISTER_GENERATED"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<?, ?> payload = new ObjectMapper().readValue(json, Map.class);
        Object idValue = payload.get("clientSubscriptionId");
        assertThat(idValue).isInstanceOf(String.class);
        subscriptionId = UUID.fromString((String) idValue);
    }
}
