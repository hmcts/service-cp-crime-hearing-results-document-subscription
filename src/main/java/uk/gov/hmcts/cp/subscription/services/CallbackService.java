package uk.gov.hmcts.cp.subscription.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;

@Service
@Slf4j
public class CallbackService {

    private final RestTemplate restTemplate;
    private final RetryTemplate retryTemplate;

    public CallbackService(
            final RestTemplate restTemplate,
            @Qualifier("retryTemplate") final RetryTemplate retryTemplate) {
        this.restTemplate = restTemplate;
        this.retryTemplate = retryTemplate;
    }

    /**
     * POSTs to the callback URL with Spring Retry (exponential backoff). Retries on failure or non-2xx response.
     * Throws CallbackUrlDeliveryException when all retries exhausted (handled by GlobalExceptionHandler).
     */
    public void post(final String url, final String pcrOutboundPayload) throws URISyntaxException {
        retryTemplate.execute(context -> {
            doPost(url, pcrOutboundPayload);
            return null;
        });
    }

    private void doPost(final String url, final String pcrOutboundPayload) throws URISyntaxException {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        final HttpEntity<String> req = new HttpEntity<>(pcrOutboundPayload, headers);
        restTemplate.postForEntity(new URI(url), req, String.class);
    }
}
