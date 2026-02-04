package uk.gov.hmcts.cp.subscription.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cp.subscription.clients.CallbackClient;
import uk.gov.hmcts.cp.subscription.model.PcrOutboundPayload;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CallbackServiceTest {
    @Mock
    CallbackClient callbackClient;

    @InjectMocks
    CallbackService callbackService;

    @Test
    void send_to_subscriber_should_post() {
        PcrOutboundPayload pcrOutboundPayload = PcrOutboundPayload.builder().build();
        callbackService.sendToSubscriber("url", pcrOutboundPayload);
        verify(callbackClient).send_notification("url", pcrOutboundPayload);
    }
}