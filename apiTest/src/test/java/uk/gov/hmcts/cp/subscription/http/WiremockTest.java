package uk.gov.hmcts.cp.subscription.http;

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
class WiremockTest {
    private final String baseUrl = "http://localhost:8090";
    private final RestClient restClient = RestClient.create();


    @Test
    void material_metadata() {
        ResponseEntity<String> response = restClient.get()
                .uri(metaDataUrl("6c198796-08bb-4803-b456-fa0c29ca6021"))
                .retrieve()
                .toEntity(String.class);
        log.info("material metadata:{}", response.getBody());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void material_metadata_timeout() {
        String timeoutId = "11111111-1111-1111-1111-111111111112";
        ResponseEntity<String> response = restClient.get()
                .uri(metaDataUrl(timeoutId))
                .retrieve()
                .toEntity(String.class);
        log.info("material metadata timeout:{}", response.getBody());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    private String metaDataUrl(String materialId) {
        return String.format("%s/material-query-api/query/api/rest/material/material/%s/metadata", baseUrl, materialId);
    }
}
