package uk.gov.hmcts.cp.hmac.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.cp.hmac.services.HmacKeyService.KeyPair;

@ExtendWith(MockitoExtension.class)
class HmacKeyStoreServiceTest {

    @Mock
    private HmacKeyService hmacKeyService;

    @InjectMocks
    private HmacKeyStoreService hmacKeyStoreService;

    @Test
    void generateAndStore_shouldDelegateToKeyService_andStoreResult() {
        UUID subscriptionId = UUID.randomUUID();
        KeyPair keyPair = new KeyPair("key-id-1", "secret-1");
        when(hmacKeyService.generateKey()).thenReturn(keyPair);

        KeyPair result = hmacKeyStoreService.generateAndStore(subscriptionId);

        assertThat(result).isEqualTo(keyPair);
        assertThat(hmacKeyStoreService.getKeyPair(subscriptionId)).isEqualTo(keyPair);
        verify(hmacKeyService).generateKey();
    }

    @Test
    void getKeyPair_shouldThrowWhenNoKeyStoredForSubscription() {
        UUID subscriptionId = UUID.randomUUID();

        assertThatThrownBy(() -> hmacKeyStoreService.getKeyPair(subscriptionId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(subscriptionId.toString());
    }
}

