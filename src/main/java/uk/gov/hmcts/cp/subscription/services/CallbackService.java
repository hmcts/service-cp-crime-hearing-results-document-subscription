package uk.gov.hmcts.cp.subscription.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.core.ConditionTimeoutException;
import org.springframework.stereotype.Service;

import uk.gov.hmcts.cp.openapi.model.EventNotificationPayload;
import uk.gov.hmcts.cp.subscription.clients.CallbackClient;
import uk.gov.hmcts.cp.subscription.config.AppProperties;

import java.time.Duration;

import static org.awaitility.Awaitility.await;

@Service
@Slf4j
@RequiredArgsConstructor
public class CallbackService {

    private final AppProperties appProperties;
    private final CallbackClient callbackClient;

    public void sendToSubscriber(final String url, final EventNotificationPayload eventNotificationPayload) {
        try {
            waitForCallbackDelivery(url, eventNotificationPayload);
        } catch (ConditionTimeoutException e) {
            throw new ConditionTimeoutException("Callback is not ready", e);
        }
    }

    private void waitForCallbackDelivery(final String url, final EventNotificationPayload eventNotificationPayload) {
        await()
                .pollInterval(Duration.ofMillis(appProperties.getCallbackRetryIntervalMilliSecs()))
                .atMost(Duration.ofMillis(appProperties.getCallbackRetryTimeoutMilliSecs()))
                .until(() -> {
                    try {
                        callbackClient.sendNotification(url, eventNotificationPayload);
                        return true;
                    } catch (Exception e) {
                        log.warn("Callback delivery failed for {}, retrying: {}", url, e.getMessage());
                        return false;
                    }
                });
    }
}
