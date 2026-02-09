package uk.gov.hmcts.cp.subscription.clients;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.cp.openapi.model.EventNotificationPayload;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpMethod.POST;

@ExtendWith(MockitoExtension.class)
class CallbackClientTest {

    @Mock
    RestTemplate restTemplate;

    @InjectMocks
    CallbackClient client;

    @Test
    void client_should_post_to_subscriber() {
        EventNotificationPayload payload = EventNotificationPayload.builder().build();
        client.sendNotification("http://subscriber", payload);
        verify(restTemplate).exchange(anyString(), eq(POST), any(HttpEntity.class), eq(String.class));
    }
}