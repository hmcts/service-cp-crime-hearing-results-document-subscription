package uk.gov.hmcts.cp.hmac.services;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cp.hmac.model.KeyPair;
import uk.gov.hmcts.cp.vault.VaultServiceProperties;

import java.security.InvalidKeyException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class HmacSigningServiceTest {
    @Mock
    VaultServiceProperties vaultServiceProperties;
    @Spy
    EncodingService encodingService = new EncodingService();

    @InjectMocks
    HmacKeyService hmacKeyService;
    @InjectMocks
    HmacSigningService hmacSigningService;

    String message = "A message";

    @Disabled
    @Test
    void real_signature_and_message_should_match() throws InvalidKeyException {
        String encodedSecret = "insert-encoded-secret-here";
        String messageJson = "{insert-json-here}";
        String signature = "signature-here";
        byte[] secret = encodingService.decodeFromBase64(encodedSecret);

        hmacSigningService.validateSignature(secret, messageJson, signature);

        String encodedSecretAgain = encodingService.encodeWithBase64(secret);
        assertThat(encodedSecretAgain).isEqualTo(encodedSecret);
    }

    @Test
    void validate_should_verify_ok() throws InvalidKeyException {
        KeyPair keyPair = hmacKeyService.generateKey();
        String signature = hmacSigningService.sign(keyPair.getSecret(), message);
        log.info("Signature:{}", signature);
        assertThat(signature).hasSize(44);
        hmacSigningService.validateSignature(keyPair.getSecret(), message, signature);
        // no exception
    }

    @Test
    void validate_should_throw_if_bad_signature() throws InvalidKeyException {
        KeyPair keyPair = hmacKeyService.generateKey();
        assertThrows(InvalidKeyException.class, () -> hmacSigningService.validateSignature(keyPair.getSecret(), message, "bad-signature"));
    }

    @Test
    void validate_should_throw_if_bad_secret() throws InvalidKeyException {
        KeyPair keyPair = hmacKeyService.generateKey();
        String signature = hmacSigningService.sign(keyPair.getSecret(), message);
        assertThat(signature).hasSize(44);
        assertThrows(InvalidKeyException.class, () -> hmacSigningService.validateSignature("bad-=secret".getBytes(), message, signature));
    }
}
