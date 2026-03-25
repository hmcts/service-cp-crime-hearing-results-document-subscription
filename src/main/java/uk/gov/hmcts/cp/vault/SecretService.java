package uk.gov.hmcts.cp.vault;

import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@AllArgsConstructor
public class SecretService {

    private VaultServiceConfig config;

    @PostConstruct
    public void debugSecrets() {
        try {
            final DefaultAzureCredential credential = new DefaultAzureCredentialBuilder()
                    .managedIdentityClientId(config.getVaultClientId().toString())
                    .build();
            log.info("debugSecrets getting secret names from {}.", config.getVaultUri());
            final SecretClient secretClient = new SecretClientBuilder()
                    .vaultUrl(config.getVaultUri())
                    .credential(credential)
                    .buildClient();
            secretClient.listPropertiesOfSecrets().forEach(s ->
                    log.info("debugSecrets found secret:{}", s.getName()));
            updateTestSecret(secretClient);
        } catch (Exception e) {
            log.error("debugSecrets failed to get secret names from {}.", config.getVaultUri(), e);
        }
    }

    /**
     * Default credential will try various methods to get credentials
     * a) Using environment variables AZURE_CLIENT_ID, AZURE_TENANANT_ID, AZURE_CLIENT_SECRET, if set
     * b) Using az cli login credentials, if available
     */
    private DefaultAzureCredential defaultCredential() {
        log.info("Getting default azure credential");
        return new DefaultAzureCredentialBuilder().build();
    }

    private void updateTestSecret(final SecretClient secretClient) {
        final String secretName = "subscription-delete-me";
        final String secretValue = "Secret set at " + LocalDateTime.now();
        log.info("Setting secret {}", secretName);
        secretClient.setSecret(new KeyVaultSecret(secretName, secretValue));
    }
}
