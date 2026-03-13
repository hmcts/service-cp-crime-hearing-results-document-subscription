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
    void post_notification() {
        final String postUrl = baseUrl + "/notifications";
        ResponseEntity<String> postResponse = restClient.post()
                .uri(postUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(notificationBody())
                .retrieve()
                .toEntity(String.class);
        assertThat(postResponse.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        log.info("postResponse:{}", postResponse.getBody());
    }

    private String notificationBody() {
        return "{\n" +
                "  \"eventId\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\n" +
                "  \"materialId\": \"123e4567-e89b-12d3-a456-426614174000\",\n" +
                "  \"eventType\": \"PRISON_COURT_REGISTER_GENERATED\",\n" +
                "  \"timestamp\": \"2024-01-15T10:30:00Z\",\n" +
                "  \"defendant\": {\n" +
                "    \"masterDefendantId\": \"8b8f1c3a-9a41-4f0f-b8a0-1c23d9e8a111\",\n" +
                "    \"name\": \"John Doe\",\n" +
                "    \"dateOfBirth\": \"1990-05-15\",\n" +
                "    \"custodyEstablishmentDetails\": {\n" +
                "      \"emailAddress\": \"prison@moj.gov.uk\"\n" +
                "    },\n" +
                "    \"cases\": [\n" +
                "      {\n" +
                "        \"urn\": \"CT98KRYCAP\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"urn\": \"AB75123123\"\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";
    }
}