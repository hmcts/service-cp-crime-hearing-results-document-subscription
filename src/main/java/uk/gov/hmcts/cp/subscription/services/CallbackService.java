package uk.gov.hmcts.cp.subscription.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.openapi.model.EventNotificationPayload;
import uk.gov.hmcts.cp.subscription.clients.CallbackClient;

@Service
@Slf4j
@RequiredArgsConstructor
public class CallbackService {

    private final CallbackClient callbackClient;

    public void sendToSubscriber(final String url, final EventNotificationPayload eventNotificationPayload) {
        callbackClient.sendNotification(url, eventNotificationPayload);
    }
}
