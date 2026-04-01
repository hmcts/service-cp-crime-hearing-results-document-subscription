package uk.gov.hmcts.cp.hmac.services;

import lombok.extern.slf4j.Slf4j;
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

    @Test
    void real_signature_and_message_should_match() throws InvalidKeyException {
        String keyId = "kid-v1-231e9a94-6cb5-4ad9-9df2-ff4f5c91d313";
        String encodedSecret = "U3R1YiBzdHJpbmcgdXNlZCBwdXJlbHkgZm9yIGRldmVsb3BtZW50IHB1cnBvc2VzLiBUbyBiZSBzZWN1cmVkLg==";
        byte[] secret = encodingService.decodeFromBase64(encodedSecret);
        String messageJson = "{\"cases\":[{\"urn\":\"string\"}],\"masterDefendantId\":\"7c198796-08bb-4803-b456-fa0c29ca6022\",\"defendantName\":\"string\",\"defendantDateOfBirth\":\"1990-05-15\",\"documentId\":\"2c1b7ce5-af3a-4cec-bd9f-ac9aa939f86b\",\"documentGeneratedTimestamp\":\"2024-01-15T10:30:00Z\",\"prisonEmailAddress\":\"string@email.com\"}";
        String signature = hmacSigningService.sign(secret, messageJson);
        hmacSigningService.validateSignature(secret, messageJson, signature);
        assertThat(signature).isEqualTo("V/0SgdUHxnw5JzUE5TmIXlmPBgKVwqvoe7aOViFo99c=");

        String encodedSecretAgain = encodingService.encodeWithBase64(secret);
        assertThat(encodedSecretAgain).isEqualTo(encodedSecret);
    }

    @Test
    void validate_should_verify_ok() throws InvalidKeyException {
        KeyPair keyPair = hmacKeyService.generateKey();
        String signature = hmacSigningService.sign(keyPair.getSecret(), message);
        log.info("Signature:{}", signature);
        assertThat(signature).isEqualTo("TkkWx3X55YaWF5KB2BwSY4LcoDnFqZFOMrB43hkuFkE=");
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
        assertThat(signature).isEqualTo("TkkWx3X55YaWF5KB2BwSY4LcoDnFqZFOMrB43hkuFkE=");
        assertThrows(InvalidKeyException.class, () -> hmacSigningService.validateSignature("bad-=secret".getBytes(), message, signature));
    }
}
