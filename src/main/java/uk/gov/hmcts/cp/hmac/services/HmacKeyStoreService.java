package uk.gov.hmcts.cp.hmac.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class HmacKeyStoreService {

    public static final String KEY_ID = "keyId";
    public static final String SECRET = "secret";
    public static final String SLASH = "/";

    private final HmacKeyService hmacKeyService;
    @Nullable
    private final VaultTemplate vaultTemplate;
    private final boolean vaultEnabled;
    private final String vaultPathPrefix;

    private final Map<UUID, HmacKeyService.KeyPair> cache = new ConcurrentHashMap<>();

    public HmacKeyStoreService(
            final HmacKeyService hmacKeyService,
            @Autowired(required = false) @Nullable final VaultTemplate vaultTemplate,
            @Value("${hmac.vault-enabled:false}") final boolean vaultEnabled,
            @Value("${hmac.vault-path:secret/hmac}") final String vaultPathPrefix) {
        this.hmacKeyService = hmacKeyService;
        this.vaultTemplate = vaultTemplate;
        this.vaultEnabled = vaultEnabled;
        this.vaultPathPrefix = vaultPathPrefix;
        if (vaultEnabled && vaultTemplate == null) {
            throw new IllegalStateException("hmac.vault-enabled is true but VaultTemplate is not available. "
                    + "Set HMAC_VAULT_ENABLED=false or ensure Spring Cloud Vault is configured.");
        }
    }

    public HmacKeyService.KeyPair generateAndStore(final UUID subscriptionId) {
        final HmacKeyService.KeyPair keyPair = hmacKeyService.generateKey();
        cache.put(subscriptionId, keyPair);
        if (vaultEnabled) {
            writeToVault(subscriptionId, keyPair);
        }
        return keyPair;
    }

    @SuppressWarnings("PMD.OnlyOneReturn")
    public HmacKeyService.KeyPair getKeyPair(final UUID subscriptionId) {
        final HmacKeyService.KeyPair fromCache = cache.get(subscriptionId);
        if (fromCache != null) {
            return fromCache;
        }
        if (vaultEnabled) {
            final HmacKeyService.KeyPair fromVault = readFromVault(subscriptionId);
            cache.put(subscriptionId, fromVault);
            return fromVault;
        }
        throw new IllegalStateException("No HMAC key for subscription " + subscriptionId);
    }

    private void writeToVault(final UUID subscriptionId, final HmacKeyService.KeyPair keyPair) {
        if (vaultTemplate == null) {
            return;
        }
        final String path = vaultPathPrefix + SLASH + subscriptionId;
        final Map<String, Object> data = Map.of(
                KEY_ID, keyPair.keyId(),
                SECRET, keyPair.secret()
        );
        vaultTemplate.write(path, data);
    }

    private HmacKeyService.KeyPair readFromVault(final UUID subscriptionId) {
        if (vaultTemplate == null) {
            throw new IllegalStateException("No HMAC key for subscription " + subscriptionId);
        }
        final String path = vaultPathPrefix + SLASH + subscriptionId;
        final VaultResponse response = vaultTemplate.read(path);
        if (response == null || response.getData() == null) {
            throw new IllegalStateException("No HMAC key in Vault for subscription " + subscriptionId);
        }
        final Object keyIdObj = response.getData().get(KEY_ID);
        final Object secretObj = response.getData().get(SECRET);
        if (keyIdObj == null || secretObj == null) {
            throw new IllegalStateException("Incomplete HMAC key data in Vault for subscription " + subscriptionId);
        }
        return new HmacKeyService.KeyPair(keyIdObj.toString(), secretObj.toString());
    }
}

