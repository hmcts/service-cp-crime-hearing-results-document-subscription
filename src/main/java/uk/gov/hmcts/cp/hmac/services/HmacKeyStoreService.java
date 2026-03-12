package uk.gov.hmcts.cp.hmac.services;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@AllArgsConstructor
public class HmacKeyStoreService implements HmacKeyStore {

    private final HmacKeyService hmacKeyService;

    private final ConcurrentMap<UUID, HmacKeyService.KeyPair> cache = new ConcurrentHashMap<>();

    @Override
    public HmacKeyService.KeyPair generateAndStore(final UUID subscriptionId) {
        final HmacKeyService.KeyPair keyPair = hmacKeyService.generateKey();
        cache.put(subscriptionId, keyPair);
        return keyPair;
    }

    @Override
    public HmacKeyService.KeyPair getKeyPair(final UUID subscriptionId) {
        final HmacKeyService.KeyPair fromCache = cache.get(subscriptionId);
        if (fromCache != null) {
            return fromCache;
        }
        throw new IllegalStateException("No HMAC key for subscription " + subscriptionId);
    }
}


