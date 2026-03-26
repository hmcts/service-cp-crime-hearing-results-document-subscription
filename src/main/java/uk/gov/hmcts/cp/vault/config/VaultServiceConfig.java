package uk.gov.hmcts.cp.vault.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@Getter
public class VaultServiceConfig {

    private final boolean vaultEnabled;
    private final String vaultUrl;
    private final String vaultClientId;

    public VaultServiceConfig(
            @Value("${vault.enabled:true}") final boolean vaultEnabled,
            @Value("${vault.uri:}") final String vaultUrl,
            @Value("${vault.client-id:}") final String vaultClientId) {
        log.info("Vault initialised with vaultEnabled:{}", vaultEnabled);
        log.info("Vault initialised with vaultUrl:\"{}\"", vaultUrl);
        log.info("Vault initialised with vaultClientId:\"{}\"", vaultClientId.isBlank() ? "(not set)" : vaultClientId);
        this.vaultEnabled = vaultEnabled;
        this.vaultUrl = vaultUrl;
        this.vaultClientId = vaultClientId;
    }
}