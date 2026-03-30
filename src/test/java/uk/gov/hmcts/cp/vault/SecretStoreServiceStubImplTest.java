package uk.gov.hmcts.cp.vault;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cp.hmac.services.EncodingService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecretStoreServiceStubImplTest {
    @Mock
    EncodingService encodingService;

    @InjectMocks
    SecretStoreServiceStubImpl secretStoreServiceStub;

    @Test
    void get_secret_should_default_to_current() {
        when(encodingService.encodeWithBase64("any-secret".getBytes())).thenReturn("any-secret-encoded");
        assertThat(secretStoreServiceStub.getSecret("any-secret")).isEqualTo(Optional.of("any-secret-encoded"));
    }
}