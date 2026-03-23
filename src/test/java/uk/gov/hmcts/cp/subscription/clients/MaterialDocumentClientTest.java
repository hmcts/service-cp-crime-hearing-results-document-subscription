package uk.gov.hmcts.cp.subscription.clients;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.GET;

@ExtendWith(MockitoExtension.class)
class MaterialDocumentClientTest {

    private static final String DOCUMENT_URL = "https://blob.core.windows.net/container/file.pdf?sig=abc";

    @Mock
    RestTemplate restTemplate;

    @InjectMocks
    MaterialDocumentClient materialDocumentClient;

    @Test
    void get_material_document_should_call_url_with_get() {
        materialDocumentClient.getMaterialDocument(DOCUMENT_URL);
        verify(restTemplate).exchange(eq(DOCUMENT_URL), eq(GET), any(HttpEntity.class), eq(byte[].class));
    }

    @Test
    void get_material_document_should_return_response_body() {
        byte[] documentBytes = "pdf-content".getBytes();
        when(restTemplate.exchange(eq(DOCUMENT_URL), eq(GET), any(HttpEntity.class), eq(byte[].class)))
                .thenReturn(ResponseEntity.ok(documentBytes));

        ResponseEntity<byte[]> result = materialDocumentClient.getMaterialDocument(DOCUMENT_URL);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(documentBytes);
    }

    @Test
    void get_material_document_should_return_full_response_entity() {
        byte[] documentBytes = new byte[]{1, 2, 3};
        ResponseEntity<byte[]> expected = ResponseEntity.ok(documentBytes);
        when(restTemplate.exchange(eq(DOCUMENT_URL), eq(GET), any(HttpEntity.class), eq(byte[].class)))
                .thenReturn(expected);

        ResponseEntity<byte[]> result = materialDocumentClient.getMaterialDocument(DOCUMENT_URL);

        assertThat(result).isEqualTo(expected);
    }
}
