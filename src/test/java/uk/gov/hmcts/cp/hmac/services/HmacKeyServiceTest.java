package uk.gov.hmcts.cp.hmac.services;

import com.azure.security.keyvault.secrets.SecretClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cp.hmac.config.HmacServiceConfig;
import uk.gov.hmcts.cp.hmac.model.KeyPair;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.cp.hmac.services.HmacKeyService.STUB_KEY_ID;
import static uk.gov.hmcts.cp.hmac.services.HmacKeyService.STUB_SECRET_STRING;

@Slf4j
@ExtendWith(MockitoExtension.class)
class HmacKeyServiceTest {

    @Mock
    private HmacServiceConfig config;

    @Mock
    private SecretClient secretClient;

    @Mock
    private EncodingService encodingService;

    private HmacKeyService service;

    @BeforeEach
    void setUp() {
        service = new HmacKeyService(config, Optional.of(secretClient), encodingService);
    }

    private static final int KEY_PAIR_ATTEMPTS = 100000;

    @Test
    void generateKey_should_generate_random_when_vault_enabled() {
        given(config.isVaultEnabled()).willReturn(true);
        given(encodingService.encodeWithBase64(any())).willReturn("base64encoded");
        KeyPair keyPair = service.generateAndStore(UUID.randomUUID());
        assertThat(keyPair.getKeyId()).matches("^kid_[a-z0-9\\-]{36}$");
        assertThat(keyPair.getSecret()).hasSize(32);
    }

    @Test
    void generateKey_should_return_distinct_when_vault_enabled() {
        PrintStream originalStdOut = System.out;
        captureStdOut();
        given(config.isVaultEnabled()).willReturn(true);
        given(encodingService.encodeWithBase64(any())).willReturn("base64encoded");
        Set<String> keyIds = new HashSet<>();
        Set<String> secrets = new HashSet<>();
        for (int n = 0; n < KEY_PAIR_ATTEMPTS; n++) {
            KeyPair keyPair = service.generateAndStore(UUID.randomUUID());
            keyIds.add(keyPair.getKeyId());
            secrets.add(new String(keyPair.getSecret()));
        }
        assertThat(keyIds).hasSize(KEY_PAIR_ATTEMPTS);
        assertThat(secrets).hasSize(KEY_PAIR_ATTEMPTS);
        System.setOut(originalStdOut);
    }

    @Test
    void generateKey_should_always_return_same_when_vault_disabled() {
        Set<String> keyIds = new HashSet<>();
        Set<String> secrets = new HashSet<>();
        UUID fixedId = UUID.randomUUID();
        for (int n = 0; n < KEY_PAIR_ATTEMPTS; n++) {
            KeyPair keyPair = service.generateAndStore(fixedId);
            keyIds.add(keyPair.getKeyId());
            secrets.add(new String(keyPair.getSecret()));
        }
        assertThat(keyIds).hasSize(1);
        assertThat(secrets).hasSize(1);
    }

    @Test
    void generateKey_should_use_hardcoded_when_vault_disabled() {
        KeyPair keyPair = service.generateAndStore(UUID.randomUUID());
        assertThat(keyPair.getKeyId()).isEqualTo(STUB_KEY_ID);
        assertThat(keyPair.getSecret()).isEqualTo(STUB_SECRET_STRING.getBytes(StandardCharsets.UTF_8));
        String encodedSecret = new EncodingService().encodeWithBase64(keyPair.getSecret());
        assertThat(encodedSecret).isEqualTo("U3R1YiBzdHJpbmcgdXNlZCBwdXJlbHkgZm9yIGRldmVsb3BtZW50IHB1cnBvc2VzLiBUbyBiZSBzZWN1cmVkLg==");
    }

    @Test
    void getKeyPair_should_return_stub_when_vault_disabled() {
        UUID subscriptionId = UUID.randomUUID();
        KeyPair keyPair = service.getKeyPair(subscriptionId);
        assertThat(keyPair.getKeyId()).isEqualTo(STUB_KEY_ID);
        assertThat(keyPair.getSecret()).isEqualTo(STUB_SECRET_STRING.getBytes(StandardCharsets.UTF_8));
        verify(secretClient, never()).getSecret(anyString());
    }

    private ByteArrayOutputStream captureStdOut() {
        final ByteArrayOutputStream capturedStdOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedStdOut, true, StandardCharsets.UTF_8));
        return capturedStdOut;
    }
}