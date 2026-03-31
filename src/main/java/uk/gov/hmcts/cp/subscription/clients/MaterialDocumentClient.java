package uk.gov.hmcts.cp.subscription.clients;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static org.springframework.http.HttpMethod.GET;

/**
 * Fetches material document bytes from a pre-parsed URI supplied by material-service.
 * The URI originates from a trusted internal service (not user input), so the
 * CodeQL SSRF alert for this class is suppressed via codeql-config.yml paths-ignore.
 */
@Service
@AllArgsConstructor
@Slf4j
public class MaterialDocumentClient {

    private final RestTemplate restTemplate;

    public ResponseEntity<byte[]> getMaterialDocument(final URI uri) {
        log.info("getMaterialDocument uri:{}", sanitizeUri(uri));
        return restTemplate.exchange(uri, GET, new HttpEntity<>(new HttpHeaders()), byte[].class);
    }

    private static String sanitizeUri(final URI uri) {
        if (uri == null) {
            return null;
        }
        return StringEscapeUtils.escapeHtml4(uri.toString());
    }
}