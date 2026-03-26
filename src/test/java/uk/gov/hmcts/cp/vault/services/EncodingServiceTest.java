package uk.gov.hmcts.cp.vault.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EncodingServiceTest {

    @InjectMocks
    Base64EncodingService base64EncodingService;

    @Test
    void bytes_should_encode_and_decode() {
        byte[] bytes = "A secret".getBytes(StandardCharsets.UTF_8);
        String encoded = base64EncodingService.encodeWithBase64(bytes);
        byte[] bytesAgain = base64EncodingService.decodeFromBase64(encoded);
        assertThat(bytesAgain).isEqualTo(bytes);
    }
}