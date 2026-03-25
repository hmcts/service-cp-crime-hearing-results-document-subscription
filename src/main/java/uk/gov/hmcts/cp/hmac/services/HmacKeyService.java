package uk.gov.hmcts.cp.hmac.services;

import com.azure.security.keyvault.secrets.SecretClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.hmac.config.HmacServiceConfig;
import uk.gov.hmcts.cp.hmac.model.KeyPair;

import java.security.SecureRandom;
import java.util.Optional;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
@Service
@AllArgsConstructor
public class HmacKeyService {

    private static final String VAULT_NAME_PREFIX = "amp";
    public static final String KEY_PREFIX = "kid_";
    public static final String HYPEN = "-";
    public static final String STUB_KEY_ID = KEY_PREFIX + "f4f5dc10-d6d8-4e94-8b02-459c4121aad0";
    public static final String STUB_SECRET_STRING = "Stub string used purely for development purposes. To be secured.";
    private static final int SECRET_BYTES_LENGTH = 32;

    private final SecureRandom secureRandom = new SecureRandom();
    private final HmacServiceConfig config;
    private final Optional<SecretClient> secretClient;
    private final EncodingService encodingService;

    public KeyPair generateAndStore(final UUID subscriptionId) {
        if (!config.isVaultEnabled()) {
            log.debug("Returning STUB keyPair");
            return KeyPair.builder().keyId(STUB_KEY_ID).secret(STUB_SECRET_STRING.getBytes(UTF_8)).build();
        }
        final String keyId = KEY_PREFIX + UUID.randomUUID();
        final byte[] secretBytes = new byte[SECRET_BYTES_LENGTH];
        secureRandom.nextBytes(secretBytes);
        final String vaultName = toVaultName(subscriptionId);
        log.info("Storing HMAC secret in vault: name={}", vaultName);
        vaultClient().setSecret(vaultName, encodingService.encodeWithBase64(secretBytes));
        return KeyPair.builder().keyId(keyId).secret(secretBytes).build();
    }

    public KeyPair getKeyPair(final UUID subscriptionId) {
        if (!config.isVaultEnabled()) {
            return KeyPair.builder().keyId(STUB_KEY_ID).secret(STUB_SECRET_STRING.getBytes(UTF_8)).build();
        }
        final String vaultName = toVaultName(subscriptionId);
        log.debug("Fetching HMAC secret from vault: name={}", vaultName);
        final String encoded = vaultClient().getSecret(vaultName).getValue();
        return KeyPair.builder().keyId(KEY_PREFIX + subscriptionId).secret(encodingService.decodeFromBase64(encoded)).build();
    }

    public static String toVaultName(final UUID subscriptionId) {
        return VAULT_NAME_PREFIX + HYPEN + subscriptionId;
    }

    private SecretClient vaultClient() {
        return secretClient.orElseThrow(() -> new IllegalStateException("Vault enabled but SecretClient not available"));
    }
}