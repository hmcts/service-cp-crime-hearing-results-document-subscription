package uk.gov.hmcts.cp.subscription.integration.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import uk.gov.hmcts.cp.subscription.integration.IntegrationTestBase;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SubscriptionControllerValidationTest extends IntegrationTestBase {

    private static final String CLIENT_SUBSCRIPTIONS = "/client-subscriptions";
    private static final String SUBSCRIPTION_REQUEST_BAD_EVENT = "stubs/requests/subscription/subscription-request-bad-event-type.json";
    private static final String SUBSCRIPTION_REQUEST_INVALID_CALLBACK = "stubs/requests/subscription/subscription-request-invalid-callback-url.json";

    @Test
    void bad_event_type_should_return_400() throws Exception {
        String body = loadPayload(SUBSCRIPTION_REQUEST_BAD_EVENT);
        mockMvc.perform(post(CLIENT_SUBSCRIPTIONS)
                        .header("Authorization", AUTHORIZATION_HEADER_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("JSON parse error: Cannot construct instance of `uk.gov.hmcts.cp.openapi.model.EventType`, problem: Unexpected value 'BAD'"));
    }

    @Test
    void callback_url_invalid_should_return_400() throws Exception {
        String body = loadPayload(SUBSCRIPTION_REQUEST_INVALID_CALLBACK);
        mockMvc.perform(post(CLIENT_SUBSCRIPTIONS)
                        .header("Authorization", AUTHORIZATION_HEADER_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}