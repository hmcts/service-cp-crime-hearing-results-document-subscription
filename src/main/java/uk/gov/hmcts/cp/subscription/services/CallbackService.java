package uk.gov.hmcts.cp.subscription.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
@RequiredArgsConstructor
public class CallbackService {

    private final RestClient restClient;
    private final RetryTemplate retryTemplate;

    public void post(final String url, final String pcrOutboundPayload) {
        retryTemplate.execute(context -> {
            doPost(url, pcrOutboundPayload);
            return null;
        });
    }

    private void doPost(final String url, final String pcrOutboundPayload) {
        restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(pcrOutboundPayload)
                .retrieve()
                .toBodilessEntity();
    }
}
