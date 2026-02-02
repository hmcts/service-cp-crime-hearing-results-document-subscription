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
        mockMvc.perform(post("/client-subscriptions")
                        .param("callbackUrl", "https://my-callback-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(""));
    }

    @Test
    void callback_url_should_return_400() throws Exception {
        String body = new ObjectMapper().writeValueAsString(request);
        mockMvc.perform(post("/client-subscriptions")
                        .param("callbackUrl", "not-a-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(""));
    }
}