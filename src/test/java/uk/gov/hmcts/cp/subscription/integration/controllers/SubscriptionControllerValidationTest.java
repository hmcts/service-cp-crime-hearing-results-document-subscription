package uk.gov.hmcts.cp.subscription.integration.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.hmcts.cp.openapi.model.ClientSubscriptionRequest;
import uk.gov.hmcts.cp.openapi.model.NotificationEndpoint;
import uk.gov.hmcts.cp.subscription.integration.IntegrationTestBase;
import uk.gov.hmcts.cp.subscription.repositories.SubscriptionRepository;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.cp.openapi.model.EventType.CUSTODIAL_RESULT;
import static uk.gov.hmcts.cp.openapi.model.EventType.PRISON_COURT_REGISTER_GENERATED;

class SubscriptionControllerValidationTest extends IntegrationTestBase {

    public static final String CLIENT_SUBSCRIPTIONS = "/client-subscriptions";
    @Autowired
    SubscriptionRepository subscriptionRepository;

    NotificationEndpoint notificationEndpoint = NotificationEndpoint.builder()
            .callbackUrl("https://my-callback-url")
            .build();
    ClientSubscriptionRequest request = ClientSubscriptionRequest.builder()
            .notificationEndpoint(notificationEndpoint)
            .eventTypes(List.of(PRISON_COURT_REGISTER_GENERATED, CUSTODIAL_RESULT))
            .build();

    @Test
    void bad_event_type_should_return_400() throws Exception {
        String body = new ObjectMapper().writeValueAsString(request)
                .replace("PRISON_COURT_REGISTER_GENERATED", "BAD");
        mockMvc.perform(post(CLIENT_SUBSCRIPTIONS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("JSON parse error: Cannot construct instance of `uk.gov.hmcts.cp.openapi.model.EventType`, problem: Unexpected value 'BAD'"));
    }

    @Test
    void callback_url_invalid_should_return_400() throws Exception {
        ClientSubscriptionRequest invalidRequest = ClientSubscriptionRequest.builder()
                .notificationEndpoint(NotificationEndpoint.builder().callbackUrl("not-a-url").build())
                .eventTypes(List.of(PRISON_COURT_REGISTER_GENERATED, CUSTODIAL_RESULT))
                .build();
        String body = new ObjectMapper().writeValueAsString(invalidRequest);
        mockMvc.perform(post(CLIENT_SUBSCRIPTIONS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}