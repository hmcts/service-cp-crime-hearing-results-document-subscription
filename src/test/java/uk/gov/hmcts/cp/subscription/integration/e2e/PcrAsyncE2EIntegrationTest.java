package uk.gov.hmcts.cp.subscription.integration.e2e;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.ResultActions;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;
import uk.gov.hmcts.cp.material.openapi.api.MaterialApi;
import uk.gov.hmcts.cp.servicebus.integration.ServiceBusTestService;
import uk.gov.hmcts.cp.servicebus.services.ServiceBusAdminService;
import uk.gov.hmcts.cp.servicebus.services.ServiceBusProcessorService;
import uk.gov.hmcts.cp.servicebus.services.ServiceBusService;
import uk.gov.hmcts.cp.subscription.config.IgnoreSSLCertificatesForWiremockTest;
import uk.gov.hmcts.cp.subscription.integration.IntegrationTestBase;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.Objects.nonNull;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.cp.servicebus.config.ServiceBusConfigService.PCR_OUTBOUND_TOPIC;
import static uk.gov.hmcts.cp.subscription.integration.helpers.JwtHelper.bearerTokenWithAzp;
import static uk.gov.hmcts.cp.subscription.integration.stubs.CallbackStub.getDocumentIdFromCallbackServeEvents;
import static uk.gov.hmcts.cp.subscription.integration.stubs.CallbackStub.stubCallbackEndpoint;
import static uk.gov.hmcts.cp.subscription.integration.stubs.CallbackStub.stubCallbackEndpointReturnsServerError;
import static uk.gov.hmcts.cp.subscription.integration.stubs.MaterialStub.stubMaterialBinary;
import static uk.gov.hmcts.cp.subscription.integration.stubs.MaterialStub.stubMaterialContent;
import static uk.gov.hmcts.cp.subscription.integration.stubs.MaterialStub.stubMaterialMetadata;
import static uk.gov.hmcts.cp.subscription.integration.stubs.MaterialStub.stubMaterialMetadataNoContent;
import static uk.gov.hmcts.cp.subscription.integration.stubs.SubscriptionStub.createSubscriptionPcr;
import static uk.gov.hmcts.cp.subscription.integration.stubs.SubscriptionStub.deleteSubscription;

@EnableWireMock({
        @ConfigureWireMock(name = "material-client", baseUrlProperties = "material-client.url", port = 0),
        @ConfigureWireMock(name = "callback-client", httpsBaseUrlProperties = "callback-client.url", httpsPort = 0)
})
@Import(IgnoreSSLCertificatesForWiremockTest.class)
@TestPropertySource(properties = {
        "servicebus.enabled=true",
        "service-bus.retry-seconds=1,2,3"})
@Slf4j
class PcrAsyncE2EIntegrationTest extends IntegrationTestBase {

    @Autowired
    ServiceBusAdminService adminService;
    @Autowired
    ServiceBusService serviceBusService;
    @Autowired
    ServiceBusProcessorService processorService;
    @Autowired
    ServiceBusTestService testService;

    private UUID subscriptionId;
    private UUID otherSubscriptionId;
    private UUID lateSubscriptionId;
    private UUID callbackDocumentId;

    private static final UUID MATERIAL_ID = UUID.fromString("6c198796-08bb-4803-b456-fa0c29ca6021");
    private static final String DOCUMENT_URI = CLIENT_SUBSCRIPTIONS_URI + "/{clientSubscriptionId}/documents/{documentId}";
    private static final String PCR_EVENT_PAYLOAD_PATH = "stubs/requests/progression/pcr-request-prison-court-register.json";
    private static final String PCR_EVENT_TIMEOUT_PATH = "stubs/requests/progression/pcr-request-material-timeout.json";
    private static final String CALLBACK_URI_OTHER = "/callback/other";
    private static final String CALLBACK_URI_LATE = "/callback/late";

    private static final String CLIENT_ID_OTHER = "22222222-2222-3333-4444-555555555555";
    private static final String CLIENT_ID_LATE = "33333333-2222-3333-4444-555555555555";


    @InjectWireMock("callback-client")
    private WireMockServer callbackWireMock;

    @Value("${callback-client.url}")
    private String callbackBaseUrl;

    @MockitoSpyBean
    private MaterialApi materialApi;

    @BeforeEach
    void setUp() {
        assumeTrue(adminService.isServiceBusReady(), "ServiceBus is not running. Run gradlew composeUp / composeDown");
        testService.dropTopicIfExists(PCR_OUTBOUND_TOPIC, "subscription1");
        adminService.createTopicAndSubscription(PCR_OUTBOUND_TOPIC, "subscription1");
        processorService.startMessageProcessor(PCR_OUTBOUND_TOPIC, "subscription1");
        WireMock.reset();
        if (nonNull(callbackWireMock)) {
            callbackWireMock.resetAll();
        }
        clearAllTables();
    }

    @Test
    void happy_path_should_return_pdf() throws Exception {
        given_i_am_a_subscriber_with_a_subscription();
        given_i_have_a_callback_endpoint();
        given_material_service_returns_document_success();

        when_a_pcr_event_is_posted();
        when_material_service_responds();

        then_the_subscriber_receives_a_callback();
        then_the_subscriber_can_retrieve_the_document();
    }


    // We need to implement different tests now that we are queuing the inbound notification and the outbound
    // notifications
    // 1) We try material-service several times
    // 2) We try outbound notification several times - DONE

    @Test
    void callback_client_not_responding_should_try_3_times_in_7_seconds() throws Exception {
        given_i_am_a_subscriber_with_a_subscription();
        given_callback_endpoint_returns_server_error();
        given_material_service_returns_document_success();

        when_a_pcr_event_is_posted();
        when_material_service_responds();

        Thread.sleep(7000);

        then_callback_was_attempted_times(3);
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

    private void given_callback_endpoint_returns_server_error() {
        stubCallbackEndpointReturnsServerError(callbackWireMock, CALLBACK_URI);
    }

    private void when_a_pcr_event_is_posted_expect_callback_delivery_timeout() throws Exception {
        postPcrEvent(PCR_EVENT_PAYLOAD_PATH)
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.error").value("gateway_timeout"))
                .andExpect(jsonPath("$.message").value("Callback is not ready"));
    }

    private void then_callback_was_attempted_times(int numberOfTries) {
        callbackWireMock.verify(exactly(numberOfTries), postRequestedFor(urlPathEqualTo(CALLBACK_URI)));
    }

    private void when_a_pcr_event_is_posted() throws Exception {
        postPcrEvent(PCR_EVENT_PAYLOAD_PATH).andExpect(status().isAccepted());
    }

    private void when_a_pcr_event_is_posted_with_timeout() throws Exception {
        postPcrEvent(PCR_EVENT_TIMEOUT_PATH)
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.error").value("gateway_timeout"))
                .andExpect(jsonPath("$.message").value("Material metadata not ready"));
    }

    private ResultActions postPcrEvent(String payloadPath) throws Exception {
        return mockMvc.perform(post(NOTIFICATIONS_PCR_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                .content(loadPayload(payloadPath)));
    }

    private void when_material_service_responds() {
        await()
                .pollInterval(Duration.ofMillis(50))
                .atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> verify(materialApi, atLeastOnce()).getMaterialMetadataByMaterialId(any(UUID.class)));
    }

    private void then_the_material_api_was_polled() {
        verify(materialApi, atLeastOnce()).getMaterialMetadataByMaterialId(eq(MATERIAL_ID_TIMEOUT));
    }

    @SneakyThrows
    private void then_the_subscriber_receives_a_callback() {
        log.info("sleeping for 1 second in Test ... it would of course be better to wait for something signalling callback is done");
        Thread.sleep(2000);
        callbackWireMock.verify(1, postRequestedFor(urlPathEqualTo(CALLBACK_URI)));
        callbackDocumentId = getDocumentIdFromCallbackServeEvents(callbackWireMock, CALLBACK_URI);
    }

    private void then_the_subscriber_does_not_receive_a_callback() {
        callbackWireMock.verify(0, postRequestedFor(urlPathEqualTo(CALLBACK_URI)));
    }

    private void then_the_subscriber_can_retrieve_the_document() throws Exception {
        getDocumentAndExpectPdf(subscriptionId, callbackDocumentId);
    }

    private void when_subscriber_loses_access() throws Exception {
        deleteSubscription(mockMvc, CLIENT_SUBSCRIPTIONS_URI, subscriptionId)
                .andExpect(status().isNoContent());
    }

    private void then_subscriber_cannot_retrieve_document() throws Exception {
        mockMvc.perform(get(DOCUMENT_URI, subscriptionId, callbackDocumentId)
                        .header("Authorization", AUTHORIZATION_HEADER_VALUE))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("invalid_request"))
                .andExpect(jsonPath("$.message").value("Access denied: subscription does not have access to this document"));
    }

    private void getDocumentAndExpectPdf(UUID subId, UUID docId) throws Exception {
        mockMvc.perform(get(DOCUMENT_URI, subId, docId)
                        .header("Authorization", AUTHORIZATION_HEADER_VALUE))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("application/pdf")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("PrisonCourtRegister")));
    }

    private void getDocumentAndExpectPdf(UUID subId, UUID docId, String clientId) throws Exception {
        mockMvc.perform(get(DOCUMENT_URI, subId, docId)
                        .header("Authorization", bearerTokenWithAzp(clientId)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("application/pdf")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("PrisonCourtRegister")));
    }

    private void createSubscription() throws Exception {
        subscriptionId = createSubscriptionPcr(mockMvc, CLIENT_SUBSCRIPTIONS_URI, callbackBaseUrl, CALLBACK_URI);
    }

    private void given_another_subscription_with_custodial_only() {
        // Directly insert a subscription with no event types to simulate a client
        // that has no access to PCR documents, bypassing API validation.
        otherSubscriptionId = insertSubscription(
                UUID.fromString(CLIENT_ID_OTHER), List.of(), callbackBaseUrl + CALLBACK_URI_OTHER).getId();
    }

    private void given_late_subscriber_with_pcr() throws Exception {
        lateSubscriptionId = createSubscriptionPcr(mockMvc, CLIENT_SUBSCRIPTIONS_URI, callbackBaseUrl, CALLBACK_URI_LATE, CLIENT_ID_LATE);
    }

    private void then_late_subscriber_can_retrieve_document() throws Exception {
        getDocumentAndExpectPdf(lateSubscriptionId, callbackDocumentId, CLIENT_ID_LATE);
    }

    private void then_other_subscription_cannot_retrieve_document() throws Exception {
        mockMvc.perform(get(DOCUMENT_URI, otherSubscriptionId, callbackDocumentId)
                        .header("Authorization", bearerTokenWithAzp(CLIENT_ID_OTHER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("invalid_request"))
                .andExpect(jsonPath("$.message").value("Access denied: subscription does not have access to this document"));
    }
}
