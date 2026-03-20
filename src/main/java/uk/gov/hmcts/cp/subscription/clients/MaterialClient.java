package uk.gov.hmcts.cp.subscription.clients;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.cp.subscription.model.MaterialMetadata;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import static org.springframework.http.HttpMethod.GET;

@Service
@Slf4j
public class MaterialClient {

    public static final String CONTENT_PATH = "/material-query-api/query/api/rest/material/material/{materialId}/content";
    public static final String METADATA_PATH = "/material-query-api/query/api/rest/material/material/{materialId}/metadata";
    public static final String CJSCPPUID_HEADER = "CJSCPPUID";

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String cjscppuid;

    public MaterialClient(
            final RestTemplate restTemplate,
            @Value("${material-client.url}") final String baseUrl,
            @Value("${material-client.cjscppuid}") final String cjscppuid) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.cjscppuid = cjscppuid;
    }

    public MaterialMetadata getMetadata(final UUID materialId) {
        log.info("Getting metadata for materialId:{}", materialId);
        final HttpHeaders headers = new HttpHeaders();
        headers.set(CJSCPPUID_HEADER, cjscppuid);
        final HttpEntity<Void> req = new HttpEntity<>(headers);
        final ResponseEntity<MaterialMetadata> response = restTemplate.exchange(
                baseUrl + METADATA_PATH, GET, req, MaterialMetadata.class, materialId);
        return response.getBody();
    }

    public String getContentUrl(final UUID materialId) {
        log.info("Getting content URL for materialId:{}", materialId);
        final HttpHeaders headers = new HttpHeaders();
        headers.set(CJSCPPUID_HEADER, cjscppuid);
        final HttpEntity<Void> req = new HttpEntity<>(headers);
        final ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + CONTENT_PATH, GET, req, String.class, materialId);
        return response.getBody();
    }

    public ResponseEntity<byte[]> getMaterialDocument(final String url) {
        final URI baseUri = URI.create(baseUrl);
        final URI uri = URI.create(url);
        final URI resolvedUri = baseUri.resolve(uri);
        if (!"https".equals(resolvedUri.getScheme()) && !"http".equals(resolvedUri.getScheme())) {
            throw new IllegalArgumentException("Invalid document URL scheme");
        }
        if (!baseUri.getHost().equalsIgnoreCase(resolvedUri.getHost())) {
            throw new IllegalArgumentException("Invalid document URL host");
        }
        try {
            final URI safeUri = new URI(
                    resolvedUri.getScheme(),
                    resolvedUri.getUserInfo(),
                    baseUri.getHost(),
                    resolvedUri.getPort(),
                    resolvedUri.getPath(),
                    resolvedUri.getQuery(),
                    resolvedUri.getFragment());
            log.info("Getting material document from host:{}", sanitizeForLog(safeUri.getHost()));
            final HttpHeaders headers = new HttpHeaders();
            headers.set(CJSCPPUID_HEADER, cjscppuid);
            headers.setContentType(MediaType.APPLICATION_JSON);
            final HttpEntity<Void> req = new HttpEntity<>(headers);
            return restTemplate.exchange(safeUri, GET, req, byte[].class);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid document URL", e);
        }
    }

    private static String sanitizeForLog(final String value) {
        return value.replace('\n', '_').replace('\r', '_');
    }
}
