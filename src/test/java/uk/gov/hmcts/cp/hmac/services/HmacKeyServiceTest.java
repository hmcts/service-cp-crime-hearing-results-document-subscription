package uk.gov.hmcts.cp.hmac.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cp.hmac.config.HmacServiceConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class HmacKeyServiceTest {

    @Mock
    private HmacServiceConfig config;

    @InjectMocks
    private HmacKeyService service;

    @BeforeEach
    void setUp() {
        given(config.isVaultEnabled()).willReturn(false);
    }

    @Test
    void generateKey_shouldReturnConfiguredValues_whenLocalVaultEnabled() {
        given(config.isVaultEnabled()).willReturn(true);
        given(config.getKeyId()).willReturn("kid-config");
        given(config.getSecret()).willReturn("secret-config");

        HmacKeyService.KeyPair keyPair = service.generateKey();

        assertThat(keyPair.keyId()).isEqualTo("kid-config");
        assertThat(keyPair.secret()).isEqualTo("secret-config");
    }

    @Test
    void generateKey_shouldGenerateRandomSecret_whenLocalVaultDisabled() {
        HmacKeyService.KeyPair keyPair1 = service.generateKey();
        HmacKeyService.KeyPair keyPair2 = service.generateKey();

        assertThat(keyPair1.keyId()).startsWith("kid_");
        assertThat(keyPair1.secret()).isNotBlank();
        assertThat(keyPair2.secret()).isNotBlank();
        assertThat(keyPair1.secret()).isNotEqualTo(keyPair2.secret());
    }
}

