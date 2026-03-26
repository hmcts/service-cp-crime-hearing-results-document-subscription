package uk.gov.hmcts.cp.vault.config;

import com.azure.security.keyvault.secrets.SecretClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.cp.vault.services.AzureSecretService;
import uk.gov.hmcts.cp.vault.services.EmulatorSecretService;

@Configuration
@RequiredArgsConstructor
public class VaultSecretClientConfig {

    private final VaultServiceConfig vaultConfig;
    private final AzureSecretService azureSecretService;
    private final EmulatorSecretService emulatorSecretService;

    @Bean
    public SecretClient secretClient() throws Exception {
        return vaultConfig.isVaultEnabled()
            ? azureSecretService.build(vaultConfig.getVaultUrl(), vaultConfig.getVaultClientId())
            : emulatorSecretService.build(vaultConfig.getVaultUrl());
    }
}
