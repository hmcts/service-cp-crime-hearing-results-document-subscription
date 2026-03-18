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
    private String baseUrl = "http://localhost:8090";
    private String callbackUrl = "/callback/notify";
    private RestClient restClient = RestClient.create();

    UUID correlationId = UUID.randomUUID();

    @Test
    void callback() {
        ResponseEntity<String> response = restClient
                .post()
                .uri(baseUrl + callbackUrl)
                .header(CORRELATION_ID_KEY, correlationId.toString())
                .retrieve()
                .toEntity(String.class);
        log.info("callback response:{}", response.getBody());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
