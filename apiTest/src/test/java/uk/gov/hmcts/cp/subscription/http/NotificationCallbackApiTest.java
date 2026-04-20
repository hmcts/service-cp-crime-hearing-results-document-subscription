package uk.gov.hmcts.cp.subscription.http;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that createNotification (POST /notifications) triggers the full async flow:
 *   inbound queue → material fetch → document saved → outbound queue → callback delivered
 *
 * Rather than asserting the HTTP call to /notifications directly, we assert its
 * observable end-to-end effect: the subscriber's /mock-callback endpoint received
 * a valid HMAC-signed callback payload.
 */
@Slf4j
class NotificationCallbackApiTest extends BaseTest {

    private static final String MOCK_CALLBACK_PATH = "/mock-callback";
    private static final String MOCK_CALLBACK_RECEIVED_PATH = "/mock-callback/received";
    private static final int POLL_MAX_ATTEMPTS = 20;
    private static final long POLL_INTERVAL_MS = 3_000;

    private UUID clientId;
    private UUID subscriptionId;

    @BeforeEach
    void setUp() {
        clearReceivedCallbacks();
        clientId = UUID.randomUUID();
        subscriptionId = createSubscription(clientId, subscriptionsBaseUrl + MOCK_CALLBACK_PATH);
        log.info("Test setup: clientId:{} subscriptionId:{}", clientId, subscriptionId);
    }

    @AfterEach
    void tearDown() {
        deleteSubscription(clientId, subscriptionId);
        clearReceivedCallbacks();
    }

    @Test
    void post_notification_should_deliver_callback_to_subscriber() {
        String correlationId = UUID.randomUUID().toString();

        ResponseEntity<String> notificationResponse = restClient.post()
                .uri(subscriptionsBaseUrl + "/notifications")
                .header(CORRELATION_ID_KEY, correlationId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(notificationBody())
                .retrieve()
                .toEntity(String.class);

        assertThat(notificationResponse.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(notificationResponse.getHeaders().getFirst(CORRELATION_ID_KEY)).isEqualTo(correlationId);
        log.info("POST /notifications accepted - polling for callback delivery...");

        JsonNode callback = pollForFirstCallback();
        assertThat(callback).as("Expected a callback to be delivered to /mock-callback but none arrived").isNotNull();

        log.info("Callback received: {}", callback);
        assertThat(callback.has("documentId")).as("Callback payload should contain documentId").isTrue();
        assertThat(callback.at("/documentId").asText()).as("documentId should be a valid UUID").matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        assertThat(callback.at("/masterDefendantId").asText())
                .isEqualTo("8b8f1c3a-9a41-4f0f-b8a0-1c23d9e8a111");
    }

    /**
     * Polls GET /mock-callback/received until at least one callback arrives or max attempts reached.
     */
    @SneakyThrows
    private JsonNode pollForFirstCallback() {
        for (int attempt = 1; attempt <= POLL_MAX_ATTEMPTS; attempt++) {
            ResponseEntity<String> response = restClient.get()
                    .uri(subscriptionsBaseUrl + MOCK_CALLBACK_RECEIVED_PATH)
                    .retrieve()
                    .toEntity(String.class);

            JsonNode callbacks = jsonMapper.toJsonNode(response.getBody());
            log.info("Poll attempt {}/{}: received {} callbacks", attempt, POLL_MAX_ATTEMPTS, callbacks.size());

            if (callbacks.size() > 0) {
                return callbacks.get(0);
            }
            Thread.sleep(POLL_INTERVAL_MS);
        }
        return null;
    }

    private void clearReceivedCallbacks() {
        restClient.delete()
                .uri(subscriptionsBaseUrl + MOCK_CALLBACK_RECEIVED_PATH)
                .retrieve()
                .toEntity(String.class);
    }

    @SneakyThrows
    private String notificationBody() {
        return new String(getClass().getClassLoader()
                .getResourceAsStream("files/notification-body.json")
                .readAllBytes());
    }
}