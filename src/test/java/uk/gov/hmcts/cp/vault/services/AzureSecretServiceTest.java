package uk.gov.hmcts.cp.vault.services;

import com.azure.security.keyvault.secrets.SecretClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AzureSecretServiceTest {

    private final AzureSecretService azureSecretService = new AzureSecretService();

    @Test
    void build_should_create_secret_client_for_valid_inputs() {
        SecretClient secretClient = azureSecretService.build(
            "https://localhost:8443/",
            "00000000-0000-0000-0000-000000000001"
        );

        assertThat(secretClient).isNotNull();
    }

    @Test
    void build_should_fail_for_invalid_vault_url() {
        assertThatThrownBy(() -> azureSecretService.build(
            "not-a-valid-url",
            "00000000-0000-0000-0000-000000000001"
        ))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
