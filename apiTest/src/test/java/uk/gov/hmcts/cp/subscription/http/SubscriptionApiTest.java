package uk.gov.hmcts.cp.subscription.http;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import uk.gov.hmcts.cp.subscription.http.util.JwtHelper;
import uk.gov.hmcts.cp.subscription.http.util.JsonMapper;

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
public class SubscriptionApiTest {
    public static final String AUTHORIZATION = "Authorization";
    public static final String CORRELATION_ID_KEY = "X-Correlation-Id";

    private String testClientId = "11111111-2222-3333-4444-555555555555";
    private String bearerToken = JwtHelper.bearerTokenWithAzp(testClientId);

    private String baseUrl = System.getProperty("app.baseUrl", "http://localhost:8082");
    private RestClient restClient = RestClient.create();

    @BeforeEach
    void beforeEach() {
        try {
            restClient.get()
                    .uri(baseUrl + "/actuator/health")
                    .retrieve();
        } catch (Exception e) {
            log.error("Service not running on 8082 - run docker compose to bring up service");
        }
    }


    @Test
    void create_subscription_without_correlation_id_should_have_one_generated_in_response() {
        String clientId = UUID.randomUUID().toString();
        String token = JwtHelper.bearerTokenWithAzp(clientId);

        ResponseEntity<String> response = restClient.post()
                .uri("http://localhost:8090/client-subscriptions")
                .header(AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(subscriptionRequestBody())
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String generatedCorrelationId = response.getHeaders().getFirst(CORRELATION_ID_KEY);
        assertThat(generatedCorrelationId).isNotNull();
        log.info("generated correlationId:{}", generatedCorrelationId);
    }

    @Test
    void round_trip_subscription_should_work_ok() throws InterruptedException {
        String correlationId = UUID.randomUUID().toString();
        // To maker this idempotent we catch the 409 use the subscriptionId in the response.
        // i.e. From 409 Conflict: "{"error":"invalid_request","message":"subscription already exist with 215767e1-3da3-470e-b8aa-f5da1d79a064"}
        try {
            UUID subscriptionId = createSubscription(correlationId);
            getSubscription(subscriptionId, correlationId);
        } catch (HttpClientErrorException e) {
            log.info("Subscription already exists ... trying to parse subscriptionId from:{}", e.getMessage());
            JsonNode jsonNode = new JsonMapper().toJsonNode(e.getResponseBodyAsString());
            String message = String.valueOf(jsonNode.get("message"));
            String subscriptionIdString = message.replaceAll("subscription already exist with ", "").replaceAll("\"", "");
            UUID subscriptionId = UUID.fromString(subscriptionIdString);
            getSubscription(subscriptionId, correlationId);
        }
    }

    private UUID createSubscription(String correlationId) {
        final String postUrl = baseUrl + "/client-subscriptions";
        ResponseEntity<String> postResult = restClient.post()
                .uri(postUrl)
                .header(AUTHORIZATION, bearerToken)
                .header(CORRELATION_ID_KEY, correlationId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(subscriptionRequestBody())
                .retrieve()
                .toEntity(String.class);
        assertThat(postResult.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(postResult.getHeaders().getFirst(CORRELATION_ID_KEY)).isEqualTo(correlationId);
        JsonNode jsonNode = new JsonMapper().toJsonNode(postResult.getBody());
        String subscriptiuonIdString = String.valueOf(jsonNode.get("clientSubscriptionId")).replaceAll("\"", "");
        return UUID.fromString(subscriptiuonIdString);
    }

    @SneakyThrows
    private String subscriptionRequestBody() {
        try (var stream = getClass().getClassLoader().getResourceAsStream("files/subscription-request.json")) {
            return new String(stream.readAllBytes());
        }
    }

    private void getSubscription(UUID subscriptionId, String correlationId) {
        final String getUrl = String.format("%s/client-subscriptions/%s", baseUrl, subscriptionId);
        ResponseEntity<String> getResult = RestClient.builder().baseUrl(getUrl)
                .defaultHeader(AUTHORIZATION, bearerToken)
                .defaultHeader(CORRELATION_ID_KEY, correlationId)
                .build()
                .get()
                .retrieve()
                .toEntity(String.class);
        assertThat(getResult.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResult.getBody()).contains("clientSubscriptionId\":\"" + subscriptionId);
        assertThat(getResult.getHeaders().getFirst(CORRELATION_ID_KEY)).isEqualTo(correlationId);
    }
}