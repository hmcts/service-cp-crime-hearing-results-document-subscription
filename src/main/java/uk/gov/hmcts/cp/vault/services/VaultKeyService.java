package uk.gov.hmcts.cp.vault.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.vault.model.KeyPair;
import uk.gov.hmcts.cp.vault.services.store.VaultSecretStore;

import java.security.SecureRandom;
import java.util.UUID;


@Slf4j
@Service
@AllArgsConstructor
public class VaultKeyService {

    private static final String SECRET_NAME_PREFIX = "amp-subscriptions";
    public static final String KEY_PREFIX = "kid-";
    public static final String HYPHEN = "-";
    private static final int SECRET_BYTES_LENGTH = 32;

    private final SecureRandom secureRandom = new SecureRandom();
    private final VaultSecretStore secretService;
    private final Base64EncodingService base64EncodingService;

    public KeyPair generateAndStore(final UUID subscriptionId) {
        final String keyId = KEY_PREFIX + subscriptionId;
        final byte[] secretBytes = new byte[SECRET_BYTES_LENGTH];
        secureRandom.nextBytes(secretBytes);
        final String secretName = toVaultSecretName(subscriptionId);
        log.info("Storing vault secret for subscription {}", subscriptionId);
        secretService.setSecret(secretName, base64EncodingService.encodeWithBase64(secretBytes));
        return KeyPair.builder().keyId(keyId).secret(secretBytes).build();
    }

    public KeyPair getKeyPair(final UUID subscriptionId) {
        final String secretName = toVaultSecretName(subscriptionId);
        log.debug("Fetching valut secret for subscription {}", subscriptionId);
        final String encoded = secretService.getSecret(secretName);
        return KeyPair.builder().keyId(KEY_PREFIX + subscriptionId).secret(base64EncodingService.decodeFromBase64(encoded)).build();
    }

    public static String toVaultSecretName(final UUID subscriptionId) {
        return SECRET_NAME_PREFIX + HYPHEN + subscriptionId;
    }
}