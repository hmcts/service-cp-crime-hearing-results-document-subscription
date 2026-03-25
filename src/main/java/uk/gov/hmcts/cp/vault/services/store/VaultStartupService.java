package uk.gov.hmcts.cp.vault.services.store;

import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@AllArgsConstructor
@ConditionalOnProperty(name = "hmac.vault-enabled", havingValue = "true")
public class VaultStartupService { //TOOD - testing purpose only

    private final SecretClient secretClient;

    @PostConstruct
    public void debugSecrets() {
        try {
            log.info("debugSecrets getting secret names from vault.");
            secretClient.listPropertiesOfSecrets().forEach(s ->
                    log.info("debugSecrets found secret:{}", s.getName()));
            updateTestSecret();
        } catch (Exception e) {
            log.error("debugSecrets failed.", e);
        }
    }

    private void updateTestSecret() {
        final String secretName = "subscription-delete-me";
        final String secretValue = "Secret set at " + LocalDateTime.now();
        log.info("Setting secret {}", secretName);
        secretClient.setSecret(new KeyVaultSecret(secretName, secretValue));
    }
}