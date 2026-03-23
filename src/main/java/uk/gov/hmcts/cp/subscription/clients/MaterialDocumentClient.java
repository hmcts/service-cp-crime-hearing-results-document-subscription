package uk.gov.hmcts.cp.subscription.clients;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import static org.springframework.http.HttpMethod.GET;

/**
 * Fetches material document bytes from a URL supplied by material-service.
 * The URL originates from a trusted internal service (not user input), so the
 * CodeQL SSRF alert for this class is suppressed via codeql-config.yml paths-ignore.
 */
@Service
@AllArgsConstructor
@Slf4j
public class MaterialDocumentClient {

    private final RestTemplate restTemplate;

    public ResponseEntity<byte[]> getMaterialDocument(final String url) {
        log.info("Getting material document");
        return restTemplate.exchange(url, GET, new HttpEntity<>(new HttpHeaders()), byte[].class);
    }
}