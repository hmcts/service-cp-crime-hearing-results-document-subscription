package uk.gov.hmcts.cp.subscription.integration.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import uk.gov.hmcts.cp.servicebus.services.ServiceBusClientService;
import uk.gov.hmcts.cp.servicebus.services.ServiceBusHandlers;
import uk.gov.hmcts.cp.subscription.integration.IntegrationTestBase;
import uk.gov.hmcts.cp.subscription.integration.helpers.JwtHelper;
import uk.gov.hmcts.cp.subscription.integration.stubs.SubscriptionStub;
import uk.gov.hmcts.cp.subscription.repositories.DocumentMappingRepository;
import uk.gov.hmcts.cp.subscription.services.CallbackDeliveryService;
import uk.gov.hmcts.cp.subscription.services.JsonMapper;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Document access rules after an inbound notification has been processed (async path simulated via
 * {@link ServiceBusHandlers} and mocked {@link ServiceBusClientService}).
 */
@EnableWireMock({@ConfigureWireMock(name = "material-client", baseUrlProperties = "material-client.url", port = 0, filesUnderClasspath = "wiremock/material-client")})
@TestPropertySource(properties = {
        "material-service.url=the-real-dev-one",
        "material-client.cjscppuid=the-real-one"
})
class NotificationDocumentAccessIntegrationTest extends IntegrationTestBase {

    private static final String NOTIFICATION_URI = "/notifications";
    private static final String CALLBACK_URL = "https://callback.example.com";
    private static final String EVENT_PAYLOAD = "stubs/requests/progression/pcr-request-prison-court-register.json";
    private static final String DOCUMENT_URI = "/client-subscriptions/{clientSubscriptionId}/documents/{documentId}";
    private static final String CALLBACK_URI_LATE = "/callback/late";
    private static final String CLIENT_ID_OTHER = "22222222-2222-3333-4444-555555555555";
    private static final String CLIENT_ID_LATE = "33333333-2222-3333-4444-555555555555";

    @MockitoBean
    private CallbackDeliveryService callbackDeliveryService;

    @MockitoBean
    private ServiceBusClientService serviceBusClientService;

    @Autowired
    private ServiceBusHandlers serviceBusHandlers;

    @Autowired
    private JsonMapper jsonMapper;

    @BeforeEach
    void setUp() {
        reset(callbackDeliveryService);
        doAnswer(invocation -> {
            serviceBusHandlers.handleMessage(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2));
            return null;
        }).when(serviceBusClientService).queueMessage(anyString(), any(), anyString(), anyInt());
        clearAllTables();
    }

    private void postNotification() throws Exception {
        mockMvc.perform(post(NOTIFICATION_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                        .content(loadPayload(EVENT_PAYLOAD)))
                .andExpect(status().isAccepted());
    }

    private UUID getSingleDocumentId() {
        return documentMappingRepository.findAll().getFirst().getDocumentId();
    }

    @Test
    void subscriber_lost_access_after_notification_should_return_403() throws Exception {
        UUID subscriptionId = insertSubscription(CALLBACK_URL, List.of("PRISON_COURT_REGISTER_GENERATED"));
        postNotification();

        mockMvc.perform(get(DOCUMENT_URI, subscriptionId, getSingleDocumentId())
                        .header("Authorization", AUTHORIZATION_HEADER_VALUE))
                .andExpect(status().isOk());

        SubscriptionStub.deleteSubscription(mockMvc, CLIENT_SUBSCRIPTIONS_URI, subscriptionId).andExpect(status().isNoContent());

        mockMvc.perform(get(DOCUMENT_URI, subscriptionId, getSingleDocumentId())
                        .header("Authorization", AUTHORIZATION_HEADER_VALUE))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("invalid_request"))
                .andExpect(jsonPath("$.message").value("Access denied: subscription does not have access to this document"));
    }

    @Test
    void other_subscription_without_event_access_should_return_403() throws Exception {
        insertSubscription(CALLBACK_URL, List.of("PRISON_COURT_REGISTER_GENERATED"));
        UUID otherSubscriptionId = insertSubscription(
                UUID.fromString(CLIENT_ID_OTHER), List.of(), "https://other.example.com/callback");
        postNotification();

        mockMvc.perform(get(DOCUMENT_URI, otherSubscriptionId, getSingleDocumentId())
                        .header("Authorization", JwtHelper.bearerTokenWithAzp(CLIENT_ID_OTHER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("invalid_request"))
                .andExpect(jsonPath("$.message").value("Access denied: subscription does not have access to this document"));
    }

    @Test
    void late_subscriber_with_same_event_type_should_retrieve_document() throws Exception {
        UUID subscriptionId = insertSubscription(CALLBACK_URL, List.of("PRISON_COURT_REGISTER_GENERATED"));
        postNotification();

        mockMvc.perform(get(DOCUMENT_URI, subscriptionId, getSingleDocumentId())
                        .header("Authorization", AUTHORIZATION_HEADER_VALUE))
                .andExpect(status().isOk());

        String responseBody = SubscriptionStub.createSubscription(
                mockMvc, CLIENT_SUBSCRIPTIONS_URI, "https://late.example.com", CALLBACK_URI_LATE, CLIENT_ID_LATE);
        UUID lateSubscriptionId = jsonMapper.getUUIDAtPath(responseBody, "/clientSubscriptionId");

        mockMvc.perform(get(DOCUMENT_URI, lateSubscriptionId, getSingleDocumentId())
                        .header("Authorization", JwtHelper.bearerTokenWithAzp(CLIENT_ID_LATE)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("application/pdf")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("PrisonCourtRegister")));
    }
}
