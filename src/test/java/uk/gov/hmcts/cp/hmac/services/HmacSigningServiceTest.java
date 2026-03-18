package uk.gov.hmcts.cp.hmac.services;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cp.hmac.config.HmacServiceConfig;
import uk.gov.hmcts.cp.hmac.model.KeyPair;

import java.security.InvalidKeyException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class HmacSigningServiceTest {
    @Mock
    HmacServiceConfig hmacServiceConfig;

    @InjectMocks
    HmacKeyService hmacKeyService;
    @InjectMocks
    HmacSigningService hmacSigningService;

    String message = "A message";

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
