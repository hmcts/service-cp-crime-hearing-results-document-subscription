package uk.gov.hmcts.cp.subscription.integration.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import uk.gov.hmcts.cp.hmac.services.EncodingService;
import uk.gov.hmcts.cp.hmac.services.HmacKeyService;
import uk.gov.hmcts.cp.subscription.entities.ClientSubscriptionEntity;
import uk.gov.hmcts.cp.subscription.integration.IntegrationTestBase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.cp.subscription.model.EntityEventType.PRISON_COURT_REGISTER_GENERATED;

class SubscriptionCreateControllerIntegrationTest extends IntegrationTestBase {

    private static final String SUBSCRIPTION_REQUEST_VALID = "stubs/requests/subscription/subscription-request-valid.json";

    @Autowired
    HmacKeyService hmacKeyService;
    @Autowired
    EncodingService encodingService;

    @BeforeEach
    void beforeEach() {
        clearClientSubscriptionTable();
    }

    @Test
    void create_subscription_should_save_subscription_with_hmac() throws Exception {
        String body = loadPayload(SUBSCRIPTION_REQUEST_VALID);
        String expectedSecret = encodingService.encodeWithBase64(hmacKeyService.generateKey().getSecret());
        mockMvc.perform(post("/client-subscriptions")
                        .header("Authorization", AUTHORIZATION_HEADER_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientSubscriptionId").exists())
                .andExpect(jsonPath("$.eventTypes.[0]").value(PRISON_COURT_REGISTER_GENERATED.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.hmac.keyId").value("kid_f4f5dc10-d6d8-4e94-8b02-459c4121aad0"))
                .andExpect(jsonPath("$.hmac.secret").value(expectedSecret));
        assertThatEventTypesAreSortedInDatabase();
    }

    @Test
    void duplicate_subscription_should_return_409_with_existing_subscription_id() throws Exception {
        String body = loadPayload(SUBSCRIPTION_REQUEST_VALID);
        mockMvc.perform(post("/client-subscriptions")
                        .header("Authorization", AUTHORIZATION_HEADER_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        List<ClientSubscriptionEntity> saved = subscriptionRepository.findAll();
        assertThat(saved).hasSize(1);
        String existingId = saved.get(0).getId().toString();

        mockMvc.perform(post("/client-subscriptions")
                        .header("Authorization", AUTHORIZATION_HEADER_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("invalid_request"))
                .andExpect(jsonPath("$.message").value("subscription already exist with " + existingId));
    }

    private void assertThatEventTypesAreSortedInDatabase() {
        List<ClientSubscriptionEntity> entities = subscriptionRepository.findAll();
        assertThat(entities).hasSize(1);
        assertThat(entities.get(0).getEventTypes().get(0)).isEqualTo(PRISON_COURT_REGISTER_GENERATED);
    }
}