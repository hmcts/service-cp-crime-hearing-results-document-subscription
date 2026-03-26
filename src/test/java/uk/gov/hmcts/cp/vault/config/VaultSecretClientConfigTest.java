package uk.gov.hmcts.cp.vault.config;

import com.azure.security.keyvault.secrets.SecretClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cp.vault.services.AzureSecretService;
import uk.gov.hmcts.cp.vault.services.EmulatorSecretService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VaultSecretClientConfigTest {

    @Mock
    private EmulatorSecretService emulatorSecretService;

    @Mock
    private AzureSecretService azureSecretService;

    @Mock
    private SecretClient emulatorSecretClient;

    @Test
    void secretClient_should_delegate_to_emulator_builder_when_vault_disabled() throws Exception {
        VaultServiceConfig vaultConfig = new VaultServiceConfig(
                false,
                "https://localhost:8443",
                "00000000-0000-0000-0000-000000000001");
        VaultSecretClientConfig clientConfig = new VaultSecretClientConfig(vaultConfig, azureSecretService, emulatorSecretService);
        when(emulatorSecretService.build("https://localhost:8443")).thenReturn(emulatorSecretClient);

        SecretClient result = clientConfig.secretClient();

        assertThat(result).isSameAs(emulatorSecretClient);
        verify(emulatorSecretService).build("https://localhost:8443");
        verify(azureSecretService, never()).build("https://localhost:8443", "00000000-0000-0000-0000-000000000001");
    }

    @Test
    void secretClient_should_build_azure_client_when_vault_enabled() throws Exception {
        VaultServiceConfig vaultConfig = new VaultServiceConfig(
                true,
                "https://example.vault.azure.net/",
                "00000000-0000-0000-0000-000000000001");
        VaultSecretClientConfig clientConfig = new VaultSecretClientConfig(vaultConfig, azureSecretService, emulatorSecretService);
        when(azureSecretService.build(
                "https://example.vault.azure.net/",
                "00000000-0000-0000-0000-000000000001")).thenReturn(emulatorSecretClient);

        SecretClient result = clientConfig.secretClient();

        assertThat(result).isSameAs(emulatorSecretClient);
        verify(azureSecretService).build(
                "https://example.vault.azure.net/",
                "00000000-0000-0000-0000-000000000001");
        verify(emulatorSecretService, never()).build("https://example.vault.azure.net/");
    }
}
