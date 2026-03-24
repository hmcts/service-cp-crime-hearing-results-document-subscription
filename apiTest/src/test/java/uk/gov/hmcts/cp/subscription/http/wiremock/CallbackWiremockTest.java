package uk.gov.hmcts.cp.subscription.http.wiremock;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.cp.subscription.http.SubscriptionApiTest.CORRELATION_ID_KEY;

/* This might seem a little odd ... testing the test framework but as well as being sure about what
   endpoints we expose, it also services to document our mocked endpoints
 */
@Slf4j
class CallbackWiremockTest {
    private String wireMockBaseUrl = "http://localhost:8090";
    // What port will wiremock expose ssl on ?
    private String wireMockHttpsBaseUrl = "https://localhost:8090";
    private String callbackUrl = "/callback/notify";
    private RestClient restClient = RestClient.create();

    UUID correlationId = UUID.randomUUID();

    @Test
    void mock_callback_client_should_respond_ok() {
        ResponseEntity<String> response = restClient
                .post()
                .uri(wireMockBaseUrl + callbackUrl)
                .header(CORRELATION_ID_KEY, correlationId.toString())
                .retrieve()
                .toEntity(String.class);
        log.info("callback response:{}", response.getBody());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void mock_https_callback_client_should_respond_ok() {
        ResponseEntity<String> response = restClient
                .post()
                .uri(wireMockHttpsBaseUrl + callbackUrl)
                .header(CORRELATION_ID_KEY, correlationId.toString())
                .retrieve()
                .toEntity(String.class);
        log.info("callback response:{}", response.getBody());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
