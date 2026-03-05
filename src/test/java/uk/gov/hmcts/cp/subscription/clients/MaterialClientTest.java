package uk.gov.hmcts.cp.subscription.clients;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.cp.notification.clients.MaterialClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpMethod.GET;

@ExtendWith(MockitoExtension.class)
class MaterialClientTest {

    @Mock
    RestTemplate restTemplate;

    @InjectMocks
    MaterialClient materialClient;

    @Test
    void get_document_should_call_material_service() {
        materialClient.getMaterialDocument("http://material-service");
        verify(restTemplate).exchange(anyString(), eq(GET), any(HttpEntity.class), eq(byte[].class));
    }
}