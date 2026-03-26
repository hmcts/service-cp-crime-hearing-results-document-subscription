package uk.gov.hmcts.cp.vault.services;

import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AzureSecretService {

    public SecretClient build(final String vaultUrl, final String vaultClientId) {
        log.info("Building Azure Key Vault SecretClient");
        DefaultAzureCredential credential = new DefaultAzureCredentialBuilder()
            .managedIdentityClientId(vaultClientId)
            .build();

        return new SecretClientBuilder()
            .vaultUrl(vaultUrl)
            .credential(credential)
            .buildClient();
    }
}
