package uk.gov.hmcts.cp.vault.services.store;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@ConditionalOnProperty(name = "hmac.vault-enabled", havingValue = "false", matchIfMissing = true)
public class StubSecretService implements VaultSecretStore { //TODO - will be removed later

    private final Map<String, String> store = new ConcurrentHashMap<>();

    @Override
    public void setSecret(final String secretName, final String secretValue) {
        log.debug("Stub storing secret");
        store.put(secretName, secretValue);
    }

    @Override
    public String getSecret(final String secretName) {
        final String value = store.get(secretName);
        if (value == null) {
            throw new IllegalStateException("Stub secret not found: " + secretName);
        }
        return value;
    }
}