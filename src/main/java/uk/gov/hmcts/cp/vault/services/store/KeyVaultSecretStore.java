package uk.gov.hmcts.cp.vault.services.store;

import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
@ConditionalOnProperty(name = "hmac.vault-enabled", havingValue = "true")
public class KeyVaultSecretStore implements VaultSecretStore {

    private final SecretClient secretClient;

    @Override
    public void setSecret(final String secretName, final String secretValue) {
        secretClient.setSecret(secretName, secretValue);
    }

    @Override
    public String getSecret(final String secretName) {
        final KeyVaultSecret secret = secretClient.getSecret(secretName);
        if (secret == null) {
            throw new IllegalStateException("Secret not found in vault: " + secretName);
        }
        return secret.getValue();
    }
}