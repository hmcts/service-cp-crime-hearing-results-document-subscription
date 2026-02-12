package uk.gov.hmcts.cp.subscription.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cp.openapi.model.EventNotificationPayload;
import uk.gov.hmcts.cp.subscription.clients.CallbackClient;
import uk.gov.hmcts.cp.subscription.config.AppProperties;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CallbackServiceTest {

    @Mock
    private AppProperties appProperties;

    @Mock
    private CallbackClient callbackClient;

    @InjectMocks
    private CallbackService callbackService;

    @Test
    void send_to_subscriber_should_post() {
        when(appProperties.getCallbackRetryIntervalMilliSecs()).thenReturn(10);
        when(appProperties.getCallbackRetryTimeoutMilliSecs()).thenReturn(1000);

        EventNotificationPayload eventNotificationPayload = EventNotificationPayload.builder().build();
        callbackService.sendToSubscriber("url", eventNotificationPayload);

        verify(callbackClient).sendNotification("url", eventNotificationPayload);
    }

    @Test
    void send_to_subscriber_should_retry_until_success() {
        when(appProperties.getCallbackRetryIntervalMilliSecs()).thenReturn(10);
        when(appProperties.getCallbackRetryTimeoutMilliSecs()).thenReturn(500);
        EventNotificationPayload payload = EventNotificationPayload.builder().build();

        doThrow(new RuntimeException("intermittent failure"))
                .doNothing()
                .when(callbackClient).sendNotification("url", payload);

        callbackService.sendToSubscriber("url", payload);

        verify(callbackClient, times(2)).sendNotification("url", payload);
    }
}