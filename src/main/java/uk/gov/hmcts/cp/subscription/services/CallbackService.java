package uk.gov.hmcts.cp.subscription.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.subscription.clients.CallbackClient;
import uk.gov.hmcts.cp.subscription.model.PcrOutboundPayload;

@Service
@Slf4j
@RequiredArgsConstructor
public class CallbackService {

    private final CallbackClient callbackClient;

    public void sendToSubscriber(final String url, final PcrOutboundPayload pcrOutboundPayload) {
        callbackClient.sendNotification(url, pcrOutboundPayload);
    }
}
