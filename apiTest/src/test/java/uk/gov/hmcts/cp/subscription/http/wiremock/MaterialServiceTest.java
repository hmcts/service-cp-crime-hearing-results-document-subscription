package uk.gov.hmcts.cp.subscription.http.wiremock;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import uk.gov.hmcts.cp.subscription.http.util.JsonMapper;

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
    private final JsonMapper jsonMapper = new JsonMapper();

    UUID correlationId = UUID.randomUUID();
    UUID materialId = UUID.randomUUID();

    @Test
    void material_metadata_should_return() {
        ResponseEntity<String> response = restClient
                .get()
                .uri(metaDataUrl(materialId))
                .header(CORRELATION_ID_KEY, correlationId.toString())
                .retrieve()
                .toEntity(String.class);
        log.info("material metadata:{}", response.getBody());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void material_metadata_should_timeout() {
        UUID timeoutId = UUID.fromString("11111111-1111-1111-1111-111111111112");
        ResponseEntity<String> response = restClient
                .get()
                .uri(metaDataUrl(timeoutId))
                .header(CORRELATION_ID_KEY, correlationId.toString())
                .retrieve()
                .toEntity(String.class);
        log.info("material metadata timeout:{}", response.getBody());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void material_full_content_with_url_should_return(){
        ResponseEntity<String> response = restClient
                .get()
                .uri(contentUrl(materialId))
                .header(CORRELATION_ID_KEY, correlationId.toString())
                .retrieve()
                .toEntity(String.class);
        log.info("material content:{}", response.getBody());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode jsonNode = jsonMapper.toJsonNode(response.getBody());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String expectedContentUrl = "\"http://localhost:8090/material-query-api/query/api/rest/material/material/6c198796-08bb-4803-b456-fa0c29ca6021/binary\"";
        assertThat(jsonNode.get("contentUrl").toString()).isEqualTo(expectedContentUrl);
    }

    private String metaDataUrl(UUID materialId) {
        String url = String.format("%s/material-query-api/query/api/rest/material/material/%s/metadata", baseUrl, materialId);
        log.info("metaDataUrl:{}", url);
        return url;
    }

    private String contentUrl(UUID materialId) {
        String url = String.format("%s/material-query-api/query/api/rest/material/material/%s/content", baseUrl, materialId);
        log.info("metaDataUrl:{}", url);
        return url;
    }
}
