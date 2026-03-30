package uk.gov.hmcts.cp.vault;

import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class SecretStoreServiceDebug {

    private VaultServiceProperties vaultServiceProperties;

    @PostConstruct
    public void debugSecrets() {
        try {
            final DefaultAzureCredential credential = new DefaultAzureCredentialBuilder()
                    .managedIdentityClientId(vaultServiceProperties.getVaultClientId().toString())
                    .build();
            final SecretClient secretClient = new SecretClientBuilder()
                    .vaultUrl(vaultServiceProperties.getVaultUri())
                    .credential(credential)
                    .buildClient();
            secretClient.listPropertiesOfSecrets().forEach(s ->
                    log.info("debugSecrets found secret:{}", s.getName()));
        } catch (Exception e) {
            log.error("debugSecrets error {}", e.getMessage());
        }
    }
}
