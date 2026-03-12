package uk.gov.hmcts.cp.hmac.services;

import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class HmacKeyStoreService implements HmacKeyStore {

    private final HmacKeyService hmacKeyService;

    private final ConcurrentMap<UUID, HmacKeyService.KeyPair> cache = new ConcurrentHashMap<>();

    public HmacKeyStoreService(final HmacKeyService hmacKeyService) {
        this.hmacKeyService = hmacKeyService;
    }

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


