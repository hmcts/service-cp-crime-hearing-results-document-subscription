package uk.gov.hmcts.cp.subscription.http.wiremock;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/* This might seem a little odd ... testing the test framework but as well as being sure about what
   endpoints we expose, it also services to document our mocked endpoints
 */
@Slf4j
class MaterialServiceTest {
    private final String baseUrl = "http://localhost:8090";
    private final String CORRELATION_ID_KEY = "X-Correlation-Id";
    private final RestClient restClient = RestClient.create();

    @Test
    void material_metadata() {
        UUID materialId = UUID.randomUUID();
        String correlationId = UUID.randomUUID().toString();
        ResponseEntity<String> response = restClient
                .get()
                .uri(metaDataUrl(materialId))
                .header(CORRELATION_ID_KEY, correlationId)
                .retrieve()
                .toEntity(String.class);
        log.info("material metadata:{}", response.getBody());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst(CORRELATION_ID_KEY)).isEqualTo(correlationId);
    }

    @Test
    void material_metadata_timeout() {
        UUID timeoutId = UUID.fromString("11111111-1111-1111-1111-111111111112");
        String correlationId = UUID.randomUUID().toString();
        ResponseEntity<String> response = restClient
                .get()
                .uri(metaDataUrl(timeoutId))
                .header(CORRELATION_ID_KEY, correlationId)
                .retrieve()
                .toEntity(String.class);
        log.info("material metadata timeout:{}", response.getBody());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    private String metaDataUrl(UUID materialId) {
        return String.format("%s/material-query-api/query/api/rest/material/material/%s/metadata", baseUrl, materialId);
    }
}
