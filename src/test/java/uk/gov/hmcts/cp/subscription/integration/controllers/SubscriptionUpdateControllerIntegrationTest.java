package uk.gov.hmcts.cp.subscription.integration.controllers;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import uk.gov.hmcts.cp.subscription.entities.ClientEntity;
import uk.gov.hmcts.cp.subscription.entities.ClientEventEntity;
import uk.gov.hmcts.cp.subscription.entities.ClientSubscriptionEntity;
import uk.gov.hmcts.cp.subscription.integration.IntegrationTestBase;
import uk.gov.hmcts.cp.subscription.integration.helpers.JwtHelper;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
class SubscriptionUpdateControllerIntegrationTest extends IntegrationTestBase {

    private static final String SUBSCRIPTION_REQUEST_VALID = "stubs/requests/subscription/subscription-request-valid.json";

    @BeforeEach
    void beforeEach() {
        clearAllTables();
    }

    @Test
    void update_client_subscription_should_update_subscription() throws Exception {
        ClientSubscriptionEntity existing = insertSubscription("https://oldendpoint", List.of("PRISON_COURT_REGISTER_GENERATED"));
        String body = loadPayload(SUBSCRIPTION_REQUEST_VALID);
        mockMvc.perform(put("/client-subscriptions/{id}", existing.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", AUTHORIZATION_HEADER_VALUE)
                        .content(body))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientSubscriptionId").value(existing.getId().toString()))
                .andExpect(jsonPath("$.eventTypes.[0]").value("PRISON_COURT_REGISTER_GENERATED"))
                .andExpect(jsonPath("$.notificationEndpoint.callbackUrl").value("https://my-callback-url"));
        assertDatabaseState();
    }

    @Test
    void update_non_existent_subscription_should_return_404() throws Exception {
        String body = loadPayload(SUBSCRIPTION_REQUEST_VALID);
        mockMvc.perform(put("/client-subscriptions/{id}", UUID.randomUUID())
                        .header("Authorization", AUTHORIZATION_HEADER_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    void update_subscription_belonging_to_different_client_should_return_404() throws Exception {
        UUID otherClientId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        ClientSubscriptionEntity otherClientSubscription = insertSubscription(
                otherClientId, List.of("PRISON_COURT_REGISTER_GENERATED"), "https://other-client.com/callback");

        String body = loadPayload(SUBSCRIPTION_REQUEST_VALID);
        mockMvc.perform(put("/client-subscriptions/{id}", otherClientSubscription.getId())
                        .header("Authorization", JwtHelper.bearerTokenWithAzp(TEST_CLIENT_ID.toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    private void assertDatabaseState() {
        List<ClientEntity> clientEntities = clientRepository.findAll();
        assertThat(clientEntities).hasSize(1);
        assertThat(clientEntities.getFirst().getSubscriptionId()).isNotNull();
        assertThat(clientEntities.getFirst().getCreatedAt()).isNotNull();
        assertThat(clientEntities.getFirst().getUpdatedAt()).isNotNull();
        assertThat(clientEntities.getFirst().getCallbackUrl()).isEqualTo("https://my-callback-url");

        List<ClientEventEntity> clientEventEntities = clientEventRepository.findAll();
        assertThat(clientEventEntities).hasSize(1);
        assertThat(clientEventEntities.getFirst().getSubscriptionId()).isNotNull();
        assertThat(clientEventEntities.getFirst().getEventTypeId()).isEqualTo(1);
    }
}