package uk.gov.hmcts.cp.vault.services;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cp.vault.model.KeyPair;
import uk.gov.hmcts.cp.vault.services.store.VaultSecretStore;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
class VaultKeyServiceTest {

    @Mock
    private VaultSecretStore secretService;

    @Mock
    private Base64EncodingService base64EncodingService;

    private VaultKeyService service;

    @BeforeEach
    void setUp() {
        service = new VaultKeyService(secretService, base64EncodingService);
    }

    private static final int KEY_PAIR_ATTEMPTS = 100000;

    @Test
    void generateAndStore_should_generate_random_key() {
        when(base64EncodingService.encodeWithBase64(any())).thenReturn("base64encoded");
        KeyPair keyPair = service.generateAndStore(UUID.randomUUID());
        assertThat(keyPair.getKeyId()).matches("^kid_[a-z0-9\\-]{36}$");
        assertThat(keyPair.getSecret()).hasSize(32);
    }

    @Test
    void generateAndStore_should_store_via_secret_service() {
        when(base64EncodingService.encodeWithBase64(any())).thenReturn("base64encoded");
        UUID subscriptionId = UUID.randomUUID();
        service.generateAndStore(subscriptionId);
        verify(secretService).setSecret(VaultKeyService.toSecretName(subscriptionId), "base64encoded");
    }

    @Test
    void generateAndStore_should_return_distinct_keys() {
        when(base64EncodingService.encodeWithBase64(any())).thenReturn("base64encoded");
        Set<String> keyIds = new HashSet<>();
        Set<String> secrets = new HashSet<>();
        for (int n = 0; n < KEY_PAIR_ATTEMPTS; n++) {
            KeyPair keyPair = service.generateAndStore(UUID.randomUUID());
            keyIds.add(keyPair.getKeyId());
            secrets.add(new String(keyPair.getSecret()));
        }
        assertThat(keyIds).hasSize(KEY_PAIR_ATTEMPTS);
        assertThat(secrets).hasSize(KEY_PAIR_ATTEMPTS);
    }

    @Test
    void getKeyPair_should_decode_secret_from_secret_service() {
        UUID subscriptionId = UUID.randomUUID();
        byte[] decoded = new byte[]{1, 2, 3};
        when(secretService.getSecret(anyString())).thenReturn("base64encoded");
        when(base64EncodingService.decodeFromBase64("base64encoded")).thenReturn(decoded);

        KeyPair keyPair = service.getKeyPair(subscriptionId);

        assertThat(keyPair.getKeyId()).isEqualTo(VaultKeyService.KEY_PREFIX + subscriptionId);
        assertThat(keyPair.getSecret()).isEqualTo(decoded);
        verify(secretService).getSecret(VaultKeyService.toSecretName(subscriptionId));
    }
}