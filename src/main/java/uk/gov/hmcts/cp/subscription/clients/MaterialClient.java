package uk.gov.hmcts.cp.subscription.clients;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.cp.subscription.model.PcrOutboundPayload;

import static org.springframework.http.HttpMethod.GET;

@Service
@AllArgsConstructor
@Slf4j
public class MaterialClient {

    private RestTemplate restTemplate;

    public ResponseEntity<byte[]> getMaterialDocument(final String url) {
        log.info("Getting material document from {}", url);
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        final HttpEntity<PcrOutboundPayload> req = new HttpEntity<>(headers);
        return restTemplate.exchange(url, GET, req, byte[].class);
    }
}
