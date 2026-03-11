package uk.gov.hmcts.cp.hmac.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

@Service
public class HmacKeyService {

    private static final int SECRET_BYTES_LENGTH = 32;
    private final SecureRandom secureRandom = new SecureRandom();

    private final boolean vaultEnabled;
    private final String configuredKeyId;
    private final String configuredSecret;

    public HmacKeyService(
            @Value("${hmac.vault-enabled:false}") final boolean vaultEnabled,
            @Value("${hmac.key-id:}") final String configuredKeyId,
            @Value("${hmac.secret:}") final String configuredSecret) {
        this.vaultEnabled = vaultEnabled;
        this.configuredKeyId = configuredKeyId;
        this.configuredSecret = configuredSecret;
    }

    @SuppressWarnings("PMD.OnlyOneReturn")
    public KeyPair generateKey() {
        if (vaultEnabled && configuredKeyId != null && !configuredKeyId.isEmpty()
                && configuredSecret != null && !configuredSecret.isEmpty()) {
            return new KeyPair(configuredKeyId, configuredSecret);
        }

        final String keyId = "kid_" + UUID.randomUUID();
        final byte[] secretBytes = new byte[SECRET_BYTES_LENGTH];
        secureRandom.nextBytes(secretBytes);
        final String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(secretBytes);
        return new KeyPair(keyId, secret);
    }

    public record KeyPair(String keyId, String secret) {}
}