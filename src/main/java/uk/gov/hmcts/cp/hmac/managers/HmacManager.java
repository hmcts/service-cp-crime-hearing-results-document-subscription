package uk.gov.hmcts.cp.hmac.managers;

import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.hmac.model.KeyPair;
import uk.gov.hmcts.cp.hmac.services.EncodingService;
import uk.gov.hmcts.cp.hmac.services.HmacKeyService;
import uk.gov.hmcts.cp.hmac.services.HmacSigningService;
import uk.gov.hmcts.cp.vault.SecretStoreServiceInterface;

import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class HmacManager {

    public static final String KEY_PREFIX = "kid_";

    private HmacKeyService hmacKeyService;
    private SecretStoreServiceInterface secretStoreService;
    private HmacSigningService hmacSigningService;
    private EncodingService encodingService;

    public KeyPair createAndStoreNewKey(final UUID subscriptionId) {
        final KeyPair keyPair = hmacKeyService.generateKey();
        final String encodedSecret = encodingService.encodeWithBase64(keyPair.getSecret());
        secretStoreService.setSecret(subscriptionId.toString(), encodedSecret);
        return keyPair;
    }

    public String getKeyId(final UUID subscriptionId) {
        return KEY_PREFIX + subscriptionId;
    }

    public String getSignature(final UUID subscriptionId, final String payload) {
        final String encodedSecret = secretStoreService.getSecret(subscriptionId.toString()).
                orElseThrow(() -> new EntityNotFoundException("no existing secret for subscriptionId:" + subscriptionId));
        final byte[] secret = encodingService.decodeFromBase64(encodedSecret);
        return hmacSigningService.sign(secret, payload);
    }
}