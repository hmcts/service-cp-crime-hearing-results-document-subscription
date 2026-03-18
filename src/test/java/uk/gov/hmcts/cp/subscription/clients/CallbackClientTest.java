package uk.gov.hmcts.cp.subscription.clients;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.cp.openapi.model.EventNotificationPayload;
import uk.gov.hmcts.cp.subscription.model.EventNotificationPayloadWrapper;

import static com.azure.core.http.ContentType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpMethod.POST;
import static uk.gov.hmcts.cp.subscription.model.EventNotificationPayloadWrapper.KEY_ID_HEADER;
import static uk.gov.hmcts.cp.subscription.model.EventNotificationPayloadWrapper.SIGNATURE_HEADER;

@ExtendWith(MockitoExtension.class)
class CallbackClientTest {

    @Mock
    RestTemplate restTemplate;

    @InjectMocks
    CallbackClient client;

    @Captor
    ArgumentCaptor<HttpEntity> captor;

    @Test
    void client_should_post_to_subscriber() {
        EventNotificationPayload payload = EventNotificationPayload.builder().build();
        EventNotificationPayloadWrapper wrapper = EventNotificationPayloadWrapper.builder()
                .payload(payload)
                .keyId("key-id")
                .signature("signature")
                .build();

        client.sendNotification("http://subscriber", wrapper);

        verify(restTemplate).exchange(eq("http://subscriber"), eq(POST), captor.capture(), eq(String.class));
        assertThat(captor.getValue().getHeaders().containsHeaderValue(CONTENT_TYPE, APPLICATION_JSON));
        assertThat(captor.getValue().getHeaders().containsHeaderValue(KEY_ID_HEADER, "key-id"));
        assertThat(captor.getValue().getHeaders().containsHeaderValue(SIGNATURE_HEADER, "signature"));
        assertThat(captor.getValue().getBody()).isEqualTo(payload);
    }
}