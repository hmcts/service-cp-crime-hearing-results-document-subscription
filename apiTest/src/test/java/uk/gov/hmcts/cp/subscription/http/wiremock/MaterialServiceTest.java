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
    private final RestClient restClient = RestClient.create();


    @Test
    void material_metadata() {
        UUID materialId = UUID.randomUUID();
        ResponseEntity<String> response = restClient
                .get()
                .uri(metaDataUrl(materialId))
                .retrieve()
                .toEntity(String.class);
        log.info("material metadata:{}", response.getBody());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void material_metadata_timeout() {
        UUID timeoutId = UUID.fromString("11111111-1111-1111-1111-111111111112");
        ResponseEntity<String> response = restClient
                .get()
                .uri(metaDataUrl(timeoutId))
                .retrieve()
                .toEntity(String.class);
        log.info("material metadata timeout:{}", response.getBody());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    private String metaDataUrl(UUID materialId) {
        return String.format("%s/material-query-api/query/api/rest/material/material/%s/metadata", baseUrl, materialId);
    }
}
