package uk.gov.hmcts.cp.hmac.services;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HmacSigningServiceTest {

    private final HmacSigningService signingService = new HmacSigningService();

    @Test
    void sign_shouldBeDeterministicForSameInput() {
        String secret = "test-secret";
        String message = "payload";

        String sig1 = signingService.sign(secret, message);
        String sig2 = signingService.sign(secret, message);

        assertThat(sig1).isNotBlank();
        assertThat(sig2).isEqualTo(sig1);
    }

    @Test
    void sign_shouldChangeWhenSecretOrMessageChanges() {
        String secret = "test-secret";
        String message = "payload";

        String base = signingService.sign(secret, message);
        String withDifferentMessage = signingService.sign(secret, "payload-2");
        String withDifferentSecret = signingService.sign("other-secret", message);

        assertThat(withDifferentMessage).isNotEqualTo(base);
        assertThat(withDifferentSecret).isNotEqualTo(base);
    }
}

