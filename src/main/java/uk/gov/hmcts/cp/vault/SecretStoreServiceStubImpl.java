package uk.gov.hmcts.cp.vault;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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

    public static String encodedSecret;

    public Optional<String> getSecret(final String secretName) {
        return Optional.of(encodedSecret);
    }

    public void setSecret(final String secretName, final String secretValue) {
        encodedSecret = secretValue;
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
