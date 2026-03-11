package uk.gov.hmcts.cp.hmac.services;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HmacKeyServiceTest {

    @Test
    void generateKey_shouldReturnConfiguredValues_whenLocalVaultEnabled() {
        HmacKeyService service = new HmacKeyService(true, "kid-config", "secret-config");

        HmacKeyService.KeyPair keyPair = service.generateKey();

        assertThat(keyPair.keyId()).isEqualTo("kid-config");
        assertThat(keyPair.secret()).isEqualTo("secret-config");
    }

    @Test
    void generateKey_shouldGenerateRandomSecret_whenLocalVaultDisabled() {
        HmacKeyService service = new HmacKeyService(false, "", "");

        HmacKeyService.KeyPair keyPair1 = service.generateKey();
        HmacKeyService.KeyPair keyPair2 = service.generateKey();

        assertThat(keyPair1.keyId()).startsWith("kid_");
        assertThat(keyPair1.secret()).isNotBlank();
        assertThat(keyPair2.secret()).isNotBlank();
        assertThat(keyPair1.secret()).isNotEqualTo(keyPair2.secret());
    }
}

