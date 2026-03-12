package uk.gov.hmcts.cp.hmac.services;

import java.util.UUID;

public interface HmacKeyStore {

    HmacKeyService.KeyPair generateAndStore(UUID subscriptionId);

    HmacKeyService.KeyPair getKeyPair(UUID subscriptionId);
}

