package uk.gov.hmcts.cp.hmac.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HmacKeyStoreServiceTest {

    @Mock
    private HmacKeyService hmacKeyService;

    @Mock
    private org.springframework.vault.core.VaultTemplate vaultTemplate;

    private HmacKeyStoreService createVaultDisabledStore() {
        return new HmacKeyStoreService(hmacKeyService, vaultTemplate, false, "secret/hmac");
    }

    @Test
    void generateAndStore_shouldDelegateToKeyService_andStoreResult() {
        UUID subscriptionId = UUID.randomUUID();
        HmacKeyService.KeyPair keyPair = new HmacKeyService.KeyPair("kid-1", "secret-1");
        when(hmacKeyService.generateKey()).thenReturn(keyPair);

        HmacKeyStoreService hmacKeyStoreService = createVaultDisabledStore();
        HmacKeyService.KeyPair result = hmacKeyStoreService.generateAndStore(subscriptionId);

        assertThat(result).isEqualTo(keyPair);
        assertThat(hmacKeyStoreService.getKeyPair(subscriptionId)).isEqualTo(keyPair);
        verify(hmacKeyService).generateKey();
    }

    @Test
    void getKeyPair_shouldThrowWhenNoKeyStoredForSubscription() {
        UUID subscriptionId = UUID.randomUUID();

        HmacKeyStoreService hmacKeyStoreService = createVaultDisabledStore();
        assertThatThrownBy(() -> hmacKeyStoreService.getKeyPair(subscriptionId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(subscriptionId.toString());
    }
}

