package uk.gov.hmcts.cp.subscription.http;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This is a bit verbose and not using sensible plugins to log, parse response etc
 * This is because these tests run inside the app context. But we dont want to bloat the app with configs or plugins
 * that are needed for api-test
 * We need to decide how to run api-tests against built docker file.
 * Probably best as a separate app with its own gradle build
 */
@Slf4j
class SubscriptionApiTest {
    private static final String AUTHORIZATION = "Authorization";

    private String testClientId = "11111111-2222-3333-4444-555555555555";
    private String bearerToken = JwtHelper.bearerTokenWithAzp(testClientId);

    private String baseUrl = System.getProperty("app.baseUrl", "http://localhost:8082");
    private RestClient http = RestClient.create();

    @BeforeEach
    void beforeEach() {
        try {
            http.get()
                    .uri(baseUrl + "/actuator/health")
                    .retrieve();
        } catch (Exception e) {
            log.error("Service not running on 8082 - run docker compose to bring up service");
        }
    }


    @Test
    void round_trip_subscription_should_work_ok() throws InterruptedException {
        // To maker this idempotent we catch the 409 use the subscriptionId in the response.
        // i.e. From 409 Conflict: "{"error":"invalid_request","message":"subscription already exist with 215767e1-3da3-470e-b8aa-f5da1d79a064"}
        try {
            UUID subscriptionId = createSubscription();
            getSubscription(subscriptionId);
        } catch (HttpClientErrorException e) {
            log.info("Subscription already exists ... trying to parse subscriptionId from:{}", e.getMessage());
            JsonNode jsonNode = new JsonMapper().toJsonNode(e.getResponseBodyAsString());
            String message = String.valueOf(jsonNode.get("message"));
            String subscriptionIdString = message.replaceAll("subscription already exist with ", "").replaceAll("\"", "");
            UUID subscriptionId = UUID.fromString(subscriptionIdString);
            getSubscription(subscriptionId);
        }
    }

    private UUID createSubscription() {
        final String postUrl = baseUrl + "/client-subscriptions";
        final String body = "{\"notificationEndpoint\":{\"callbackUrl\":\"https://my-callback-url\"},\"eventTypes\":[\"PRISON_COURT_REGISTER_GENERATED\"]}";
        ResponseEntity<String> postResult = http.post()
                .uri(postUrl)
                .header(AUTHORIZATION, bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(String.class);
        assertThat(postResult.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode jsonNode = new JsonMapper().toJsonNode(postResult.getBody());
        assertThat(jsonNode.get("keyId").asText()).isNotBlank();
        assertThat(jsonNode.get("secret").asText()).isNotBlank();
        String subscriptionIdString = String.valueOf(jsonNode.get("clientSubscriptionId")).replaceAll("\"", "");
        return UUID.fromString(subscriptionIdString);
    }

    private void getSubscription(UUID subscriptionId) {
        final String getUrl = String.format("%s/client-subscriptions/%s", baseUrl, subscriptionId);
        ResponseEntity<String> getResult = RestClient.builder().baseUrl(getUrl)
                .defaultHeader(AUTHORIZATION, bearerToken)
                .build()
                .get()
                .retrieve()
                .toEntity(String.class);
        assertThat(getResult.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResult.getBody()).contains("clientSubscriptionId\":\"" + subscriptionId);
        JsonNode jsonNode = new JsonMapper().toJsonNode(getResult.getBody());
        assertThat(jsonNode.has("secret")).isFalse();
        assertThat(jsonNode.has("keyId")).isFalse();
    }
}