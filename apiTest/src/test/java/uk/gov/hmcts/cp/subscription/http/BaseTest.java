package uk.gov.hmcts.cp.subscription.http;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import uk.gov.hmcts.cp.subscription.http.util.JwtHelper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class BaseTest {
    private static final String AUTHORIZATION = "Authorization";
    private static final String CORRELATION_ID_KEY = "X-Correlation-Id";

    protected RestClient restClient = RestClient.create();
    private String subscriptionsBaseUrl = "http://localhost:8082";

    protected UUID createSubscription(UUID clientId, String callbackUrl) {
        String token = JwtHelper.bearerTokenWithAzp(clientId);
        ResponseEntity<String> response = restClient.post()
                .uri(subscriptionsBaseUrl + "/client-subscriptions")
                .header(AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(subscriptionRequestBody(callbackUrl))
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String correlationId = response.getHeaders().getFirst(CORRELATION_ID_KEY);
        assertThat(correlationId).isNotNull();
        return UUID.fromString(correlationId);
    }

    @SneakyThrows
    private String subscriptionRequestBody(String callbackUrl) {
        String filesBase = "./src/test/resources/files/";
        String json = Files.readString(Path.of(filesBase + "subscription-request.json"));
        return json.replace("https://my-callback-url", callbackUrl);
    }
}
