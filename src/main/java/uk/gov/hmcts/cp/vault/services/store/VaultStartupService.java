package uk.gov.hmcts.cp.vault.services.store;

import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.cp.vault.config.VaultConfig;

import java.time.LocalDateTime;

@Slf4j
@Component
@AllArgsConstructor
@ConditionalOnProperty(name = "hmac.vault-enabled", havingValue = "true")
public class VaultStartupService { //TODO - testing purpose only

    private final SecretClient secretClient;
    private final VaultConfig vaultConfig;

    @PostConstruct
    public void debugSecrets() {
        log.info("VaultStartupService connecting to vault url:{}", vaultConfig.getVaultUrl());
        try {
            secretClient.listPropertiesOfSecrets().forEach(s ->
                    log.info("VaultStartupService found secret:{}", s.getName()));
            updateTestSecret();
        } catch (Exception e) {
            log.error("VaultStartupService failed to connect to vault url:{}", vaultConfig.getVaultUrl(), e);
        }
    }

    private void updateTestSecret() {
        final String secretName = "subscription-delete-me";
        final String secretValue = "Secret set at " + LocalDateTime.now();
        log.info("VaultStartupService setting secret:{}", secretName);
        secretClient.setSecret(new KeyVaultSecret(secretName, secretValue));
    }
}