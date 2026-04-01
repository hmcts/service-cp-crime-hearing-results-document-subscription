package uk.gov.hmcts.cp.vault;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.hmac.model.KeyPair;
import uk.gov.hmcts.cp.hmac.services.EncodingService;
import uk.gov.hmcts.cp.hmac.services.HmacKeyService;

import java.security.InvalidKeyException;
import java.util.Optional;

import static uk.gov.hmcts.cp.vault.SecretStoreServiceAzureImpl.SECRET_PREFIX;

@Slf4j
@Service
@AllArgsConstructor
public class SecretStoreServiceStubImpl implements SecretStoreServiceInterface {
    public static final String STUB_SECRET_STRING = "Stub string used purely for development purposes. To be secured.";

    private HmacKeyService hmacKeyService;

    public static byte[] secret;
    private EncodingService encodingService;

    public Optional<String> getSecret(final String secretName) {
        if (secret == null) {
            final KeyPair keyPair = hmacKeyService.generateKey();
            secret = keyPair.getSecret();
            log.info("COLING created new secret {}", encodingService.encodeWithBase64(secret));
        }
        final String encoded = encodingService.encodeWithBase64(secret);
        return Optional.of(encoded);
    }

    public void setSecret(final String secretName, final String secretValue) {
        // Do nothing we return the stubbed secret when we call getSecret()
    }

    @SneakyThrows
    @Override
    public String getFullSecretName(final String secretName) {
        if (secretName.matches("^[a-zA-Z0-9\\-]+$")) {
            return String.format("%s-%s", SECRET_PREFIX, secretName);
        }
        throw new InvalidKeyException("Secret name can only contain alphanumerics plus \"-\"");
    }
}
