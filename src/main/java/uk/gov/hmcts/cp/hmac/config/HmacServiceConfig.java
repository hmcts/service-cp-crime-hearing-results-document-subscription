package uk.gov.hmcts.cp.hmac.config;

import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@Getter
public class HmacServiceConfig {

    private final boolean vaultEnabled;
    private final String vaultUrl;
    private final String vaultClientId;

    public HmacServiceConfig(
            @Value("${hmac.vault-enabled:false}") final boolean vaultEnabled,
            @Value("${hmac.vault-url:}") final String vaultUrl,
            @Value("${hmac.vault-client-id:}") final String vaultClientId) {
        log.info("Hmac initialised with vaultEnabled:{}", vaultEnabled);
        log.info("Hmac initialised with vaultUrl:\"{}\"", vaultUrl);
        log.info("Hmac initialised with vaultClientId:\"{}\"", vaultClientId.isBlank() ? "(not set)" : vaultClientId);
        this.vaultEnabled = vaultEnabled;
        this.vaultUrl = vaultUrl;
        this.vaultClientId = vaultClientId;
    }

    @Bean
    @ConditionalOnProperty(name = "hmac.vault-enabled", havingValue = "true")
    public SecretClient secretClient() {
        log.info("Building Azure Key Vault SecretClient");
        final DefaultAzureCredential credential = new DefaultAzureCredentialBuilder()
                .managedIdentityClientId(vaultClientId)
                .build();
        return new SecretClientBuilder()
                .vaultUrl(vaultUrl)
                .credential(credential)
                .buildClient();
    }
}