package uk.gov.hmcts.cp.subscription.http.wiremock;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/* This might seem a little odd ... testing the test framework but as well as being sure about what
   endpoints we expose, it also services to document our mocked endpoints
 */
@Slf4j
class CallbackWiremockTest {
    private String baseUrl = "http://localhost:8090";
    private String callbackUrl = "/callback/notify";
    private RestClient restClient = RestClient.create();


    @Test
    void callback() {
        ResponseEntity<String> response = restClient
                .post()
                .uri(baseUrl + callbackUrl)
                .retrieve()
                .toEntity(String.class);
        log.info("callback response:{}", response.getBody());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
