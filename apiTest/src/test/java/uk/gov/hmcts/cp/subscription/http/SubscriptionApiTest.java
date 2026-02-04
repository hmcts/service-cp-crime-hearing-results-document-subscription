package uk.gov.hmcts.cp.subscription.http;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This is a bit verbose and not using sensible plugins to log, parse response etc
 * This is because these tests run inside the app context. But we dont want to bloat the app with configs or plugins
 * that are needed for api-test
 * We need to decide how to run api-tests against built docker file.
 * Probably best as a separate app with its own gradle build
 */
class SubscriptionApiTest {
    private final String baseUrl = System.getProperty("app.baseUrl", "http://localhost:8082");
    private final RestClient http = RestClient.create();

    @Test
    void round_trip_subscription_should_work_ok() throws InterruptedException {
        final String callbackUrl = "https://my-callback-url";
        final String postUrl = String.format("%s/client-subscriptions?callbackUrl=%s", baseUrl, callbackUrl);
        final String body = "{\"eventTypes\":[\"PRISON_COURT_REGISTER_GENERATED\",\"CUSTODIAL_RESULT\"]}";
        final var postResult = http.post()
                .uri(postUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(String.class);
        assertThat(postResult.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(postResult.getBody()).contains("clientSubscriptionId");
        final String subscriptionId = postResult.getBody()
                .replaceAll(".*clientSubscriptionId\":\"", "")
                .replaceAll("\".*$", "");

        final String getUrl = String.format("%s/client-subscriptions/%s", baseUrl, subscriptionId);
        final var getResult = http.get()
                .uri(getUrl)
                .retrieve()
                .toEntity(String.class);
        assertThat(getResult.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResult.getBody()).contains("clientSubscriptionId\":\"" + subscriptionId);
    }
}