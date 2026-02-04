package uk.gov.hmcts.cp.subscription.clients;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.cp.subscription.model.PcrOutboundPayload;

import static org.springframework.http.HttpMethod.POST;

@Service
@AllArgsConstructor
public class CallbackClient {

    private RestTemplate restTemplate;

    public void send_notification(final String url, final PcrOutboundPayload subscriberOutboundPayload) {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        final HttpEntity<PcrOutboundPayload> req = new HttpEntity<>(subscriberOutboundPayload, headers);
        restTemplate.exchange(url, POST, req, String.class);
    }

    // Srivani changes to parent service

//    public void post(final String url, final String pcrOutboundPayload) {
//        retryTemplate.execute(context -> {
//            doPost(url, pcrOutboundPayload);
//            return null;
//        });
//    }
//
//    private void doPost(final String url, final String pcrOutboundPayload) {
//        restClient.post()
//                .uri(url)
//                .contentType(MediaType.APPLICATION_JSON)
//                .body(pcrOutboundPayload)
//                .retrieve()
//                .toBodilessEntity();
}
