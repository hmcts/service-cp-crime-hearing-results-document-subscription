package uk.gov.hmcts.cp.vault;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SecretServiceTest {
    @Mock
    VaultServiceConfig config;

    @InjectMocks
    SecretService secretService;

    @Test
    void secrets_should_connect_and_be_listed() {
        secretService.debugSecrets();

        // no assertions ... check logs in dev environment
    }
}