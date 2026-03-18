package uk.gov.hmcts.cp.hmac.services;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.hmac.config.HmacServiceConfig;
import uk.gov.hmcts.cp.hmac.model.KeyPair;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

@Service
@AllArgsConstructor
public class HmacKeyService {

    private static final int SECRET_BYTES_LENGTH = 32;
    public static final String STUB_KEY_ID = "kid_" + "f4f5dc10-d6d8-4e94-8b02-459c4121aad0";
    public static final String STUB_SECRET = "MBi30Xgjr4hidOeEgpXrzA_fPCn7787_jcfydsBWgoQ";

    private final SecureRandom secureRandom = new SecureRandom();

    private final HmacServiceConfig config;

    @SuppressWarnings("PMD.OnlyOneReturn")
    public KeyPair generateKey() {
        if (config.isVaultEnabled()) {
            final String keyId = "kid_" + UUID.randomUUID();
            final byte[] secretBytes = new byte[SECRET_BYTES_LENGTH];
            secureRandom.nextBytes(secretBytes);
            final String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(secretBytes);
            return KeyPair.builder().keyId(keyId).secret(secret).build();
        } else {
            return KeyPair.builder().keyId(STUB_KEY_ID).secret(STUB_SECRET).build();
        }
    }
}