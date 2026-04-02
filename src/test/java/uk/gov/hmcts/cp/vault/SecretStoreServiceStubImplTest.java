package uk.gov.hmcts.cp.vault;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.InvalidKeyException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class SecretStoreServiceStubImplTest {

    @InjectMocks
    SecretStoreServiceStubImpl secretStoreServiceStub;

    @Test
    void secret_name_should_throw_if_bad_characters() {
        secretStoreServiceStub.getFullSecretName("ok-name");                           // OK No exception
        secretStoreServiceStub.getFullSecretName(UUID.randomUUID().toString());        // OK No exception
        assertThrows(InvalidKeyException.class, () -> secretStoreServiceStub.getFullSecretName("bad.name"));
    }

    @Test
    void get_secret_should_return_the_static_secret() {
        secretStoreServiceStub.setSecret("any-secret", "any-secret-encoded");
        assertThat(secretStoreServiceStub.getSecret("any-secret")).isEqualTo(Optional.of("any-secret-encoded"));
    }
}