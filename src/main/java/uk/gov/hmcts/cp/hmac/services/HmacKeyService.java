package uk.gov.hmcts.cp.hmac.services;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.hmac.config.HmacServiceConfig;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

@Service
@AllArgsConstructor
public class HmacKeyService {

    private static final int SECRET_BYTES_LENGTH = 32;
    private final SecureRandom secureRandom = new SecureRandom();

    private final HmacServiceConfig config;

    @SuppressWarnings("PMD.OnlyOneReturn")
    public KeyPair generateKey() {
        if (config.isVaultEnabled()
                && config.getKeyId() != null && !config.getKeyId().isEmpty()
                && config.getSecret() != null && !config.getSecret().isEmpty()) {
            return new KeyPair(config.getKeyId(), config.getSecret());
        }

        final String keyId = "kid_" + UUID.randomUUID();
        final byte[] secretBytes = new byte[SECRET_BYTES_LENGTH];
        secureRandom.nextBytes(secretBytes);
        final String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(secretBytes);
        return new KeyPair(keyId, secret);
    }

    public record KeyPair(String keyId, String secret) {}
}