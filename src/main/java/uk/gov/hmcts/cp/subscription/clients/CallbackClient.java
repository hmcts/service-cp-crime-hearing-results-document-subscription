package uk.gov.hmcts.cp.subscription.clients;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.cp.subscription.model.PcrOutboundPayload;

import static org.springframework.http.HttpMethod.POST;

@Service
@AllArgsConstructor
@Slf4j
public class CallbackClient {

    private RestTemplate restTemplate;

    public void sendNotification(final String url, final PcrOutboundPayload subscriberOutboundPayload) {
        log.info("Sending notification to {}", url);
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        final HttpEntity<PcrOutboundPayload> req = new HttpEntity<>(subscriberOutboundPayload, headers);
        restTemplate.exchange(url, POST, req, String.class);
    }
}
