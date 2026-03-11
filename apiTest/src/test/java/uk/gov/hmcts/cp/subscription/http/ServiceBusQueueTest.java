package uk.gov.hmcts.cp.subscription.http;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lets send a notification to the notification service without any subscribers
 * We should still poll material service waiting for the material document to be created
 * But then not send any notifications
 */
@Slf4j
class ServiceBusQueueTest {
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
        postNotification();
    }

    private void postNotification() {
        final String postUrl = baseUrl + "/notifications";
        final String body = "{\"notificationEndpoint\":{\"callbackUrl\":\"https://my-callback-url\"},\"eventTypes\":[\"PRISON_COURT_REGISTER_GENERATED\"]}";
        ResponseEntity<String> postResult = http.post()
                .uri(postUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(String.class);
        assertThat(postResult.getStatusCode()).isEqualTo(HttpStatus.CREATED);
//        JsonNode jsonNode = new JsonMapper().toJsonNode(postResult.getBody());
//        String subscriptiuonIdString = String.valueOf(jsonNode.get("clientSubscriptionId")).replaceAll("\"", "");
//        return UUID.fromString(subscriptiuonIdString);
    }
}