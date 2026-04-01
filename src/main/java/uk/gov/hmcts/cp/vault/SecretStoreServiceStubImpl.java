package uk.gov.hmcts.cp.vault;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.hmac.services.EncodingService;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.util.Optional;

import static uk.gov.hmcts.cp.vault.SecretStoreServiceAzureImpl.SECRET_PREFIX;

@Slf4j
@Service
@AllArgsConstructor
public class SecretStoreServiceStubImpl implements SecretStoreServiceInterface {
    public static final String STUB_KEY_ID = "kid-" + "f4f5dc10-d6d8-4e94-8b02-459c4121aad0";
    public static final String STUB_SECRET_STRING = "Stub string used purely for development purposes. To be secured.";

    private EncodingService encodingService;

    public Optional<String> getSecret(final String secretName) {
        final String encoded = encodingService.encodeWithBase64(STUB_SECRET_STRING.getBytes(StandardCharsets.UTF_8));
        log.info("COLING stub getSecret returning {}", encoded);
        return Optional.of(encoded);
    }

    public void setSecret(final String secretName, final String secretValue) {
        log.info("COLING stub storing secret {} to {}", secretName, secretValue);
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
