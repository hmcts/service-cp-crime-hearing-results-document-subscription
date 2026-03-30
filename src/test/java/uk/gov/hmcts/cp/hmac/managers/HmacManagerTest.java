package uk.gov.hmcts.cp.hmac.managers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cp.hmac.services.EncodingService;
import uk.gov.hmcts.cp.hmac.services.HmacSigningService;
import uk.gov.hmcts.cp.vault.SecretStoreServiceAzureImpl;
import uk.gov.hmcts.cp.vault.VaultServiceProperties;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HmacManagerTest {

    @Mock
    VaultServiceProperties vaultServiceProperties;
    @Mock
    SecretStoreServiceAzureImpl secretStoreService;
    @Mock
    EncodingService encodingService;
    @Mock
    HmacSigningService hmacSigningService;

    @InjectMocks
    HmacManager hmacManager;

    UUID subscriptionId = UUID.randomUUID();

    @Test
    void get_key_id_should_return_kid_subscription_id() {
        assertThat(hmacManager.createAndStoreNewKey(subscriptionId)).isEqualTo("kid_f4f5dc10-d6d8-4e94-8b02-459c4121aad0");
    }

    @Test
    void get_key_id_should_contain_subscription_id() {
        when(vaultServiceProperties.isVaultEnabled()).thenReturn(true);
        assertThat(hmacManager.getKeyId(subscriptionId)).isEqualTo("kid_" + subscriptionId);
    }

    @Test
    void get_signature_should_return_signature() {
        when(secretStoreService.getSecret(subscriptionId.toString())).thenReturn(Optional.of("encodedSecret"));
        when(encodingService.decodeFromBase64("encodedSecret")).thenReturn("secret".getBytes());
        when(hmacSigningService.sign("secret".getBytes(), "payload")).thenReturn("signature");

        String signature = hmacManager.getSignature(subscriptionId, "payload");

        assertThat(signature).isEqualTo("signature");
    }
}