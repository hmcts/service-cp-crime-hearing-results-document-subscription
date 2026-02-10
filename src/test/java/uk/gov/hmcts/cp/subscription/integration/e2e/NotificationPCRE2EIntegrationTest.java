package uk.gov.hmcts.cp.subscription.integration.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

import com.github.tomakehurst.wiremock.WireMockServer;

import uk.gov.hmcts.cp.material.openapi.api.MaterialApi;
import uk.gov.hmcts.cp.subscription.config.SSLTrustingRestTemplateConfig;
import uk.gov.hmcts.cp.subscription.integration.IntegrationTestBase;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.Objects.nonNull;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EnableWireMock({
        @ConfigureWireMock(name = "material-client", baseUrlProperties = "material-client.url", port = 0),
        @ConfigureWireMock(name = "callback-client", httpsBaseUrlProperties = "callback-client.url", httpsPort = 0)
})
@ActiveProfiles("test")
@Import(SSLTrustingRestTemplateConfig.class)
class NotificationPcrE2EIntegrationTest extends IntegrationTestBase {

    private UUID subscriptionId;
    private UUID callbackDocumentId;
    private static final UUID MATERIAL_ID = UUID.fromString("6c198796-08bb-4803-b456-fa0c29ca6021");
    private static final UUID MATERIAL_ID_TIMEOUT = UUID.fromString("11111111-1111-1111-1111-111111111112");
    private static final String NOTIFICATIONS_PCR_URI = "/notifications/pcr";
    private static final String CLIENT_SUBSCRIPTIONS_URI = "/client-subscriptions";
    private static final String CALLBACK_URI = "/callback/notify";
    private static final String DOCUMENT_URI = CLIENT_SUBSCRIPTIONS_URI + "/{clientSubscriptionId}/documents/{documentId}";
    private static final String SUBSCRIPTION_REQUEST_E2E = "stubs/requests/subscription/subscription-pcr-request.json";
    private static final String PCR_EVENT_PAYLOAD_PATH = "stubs/requests/progression-pcr/pcr-request-prison-court-register.json";
    private static final String PCR_EVENT_TIMEOUT_PATH = "stubs/requests/progression-pcr/pcr-request-material-timeout.json";

    @InjectWireMock("callback-client")
    private WireMockServer callbackWireMock;

    @Value("${callback-client.url}")
    private String callbackBaseUrl;

    @MockitoSpyBean
    private MaterialApi materialApi;

    @BeforeEach
    void setUp() {
        WireMock.reset();
        if (nonNull(callbackWireMock)) {
            callbackWireMock.resetAll();
        }
        clearAllTables();
    }

    @Test
    void test_document_retrieval_success() throws Exception {
        given_i_am_a_subscriber_with_a_subscription();
        given_i_have_a_callback_endpoint();
        given_material_service_returns_document_success();

        when_a_pcr_event_is_posted();
        when_material_service_responds();

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
    }

    private void given_i_am_a_subscriber_with_a_subscription() throws Exception {
        createSubscription();
    }

    private void given_i_have_a_callback_endpoint() throws IOException {
        stubCallbackEndpoint(callbackWireMock, CALLBACK_URI);
    }

    private void given_material_service_returns_document_success() throws IOException {
        stubMaterialMetadata(MATERIAL_ID);
        stubMaterialContent(MATERIAL_ID);
        stubMaterialBinary(MATERIAL_ID);
    }

    private void given_material_service_returns_document_not_found() {
        stubMaterialMetadataNoContent(MATERIAL_ID_TIMEOUT);
    }

    private void when_a_pcr_event_is_posted() throws Exception {
        postPcrEvent(PCR_EVENT_PAYLOAD_PATH).andExpect(status().isAccepted());
    }

    private void when_a_pcr_event_is_posted_with_timeout() throws Exception {
        postPcrEvent(PCR_EVENT_TIMEOUT_PATH)
                .andExpect(status().isGatewayTimeout())
                .andExpect(content().string("Material metadata not ready"));
    }

    private ResultActions postPcrEvent(String payloadPath) throws Exception {
        return mockMvc.perform(post(NOTIFICATIONS_PCR_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                        .content(loadPayload(payloadPath)))
                .andDo(print());
    }

    private void when_material_service_responds() {
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(materialApi, atLeastOnce()).getMaterialMetadataByMaterialId(any(UUID.class)));
    }

    private void then_the_material_api_was_polled() {
        verify(materialApi, atLeastOnce()).getMaterialMetadataByMaterialId(eq(MATERIAL_ID_TIMEOUT));
    }

    private void then_the_subscriber_receives_a_callback() {
        callbackWireMock.verify(1, postRequestedFor(urlPathEqualTo(CALLBACK_URI)));
        callbackDocumentId = getDocumentIdFromCallbackServeEvents();
    }

    private UUID getDocumentIdFromCallbackServeEvents() {
        return callbackWireMock.getAllServeEvents().stream()
                .map(ServeEvent::getRequest)
                .filter(r -> nonNull(r.getUrl()) && r.getUrl().contains(CALLBACK_URI))
                .map(r -> parseDocumentIdFromBody(r.getBodyAsString()))
                .findFirst()
                .orElseThrow(() ->
                        new AssertionError("Callback request body did not contain documentId"));
    }

    private UUID parseDocumentIdFromBody(String body) {
        if (body == null || body.isBlank()) return null;
        try {
            return Optional.ofNullable(new ObjectMapper().readTree(body).get("documentId"))
                    .map(JsonNode::asText)
                    .map(UUID::fromString)
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private void then_the_subscriber_does_not_receive_a_callback() {
        callbackWireMock.verify(0, postRequestedFor(urlPathEqualTo(CALLBACK_URI)));
    }

    private void then_the_subscriber_can_retrieve_the_document() throws Exception {
        getDocumentAndExpectPdf(subscriptionId, callbackDocumentId);
    }

    private void getDocumentAndExpectPdf(UUID subId, UUID docId) throws Exception {
        mockMvc.perform(get(DOCUMENT_URI, subId, docId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("application/pdf")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("PrisonCourtRegister")))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    private void createSubscription() throws Exception {
        String callbackUrl = callbackBaseUrl.endsWith("/") ? callbackBaseUrl + CALLBACK_URI.substring(1) : callbackBaseUrl + CALLBACK_URI;
        String body = loadPayload(SUBSCRIPTION_REQUEST_E2E).replace("{{callback.url}}", callbackUrl);
        String json = postSubscriptionAndReturnJson(body);
        subscriptionId = UUID.fromString(new ObjectMapper().readTree(json).get("clientSubscriptionId").asText());
    }

    private String postSubscriptionAndReturnJson(String body) throws Exception {
        return mockMvc.perform(post(CLIENT_SUBSCRIPTIONS_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientSubscriptionId").exists())
                .andReturn().getResponse().getContentAsString();
    }
}
