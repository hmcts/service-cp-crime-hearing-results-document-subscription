package uk.gov.hmcts.cp.hmac.services;

import java.util.UUID;

import static uk.gov.hmcts.cp.hmac.services.HmacKeyService.KeyPair;

public interface HmacKeyStore {

    KeyPair generateAndStore(UUID subscriptionId);

    KeyPair getKeyPair(UUID subscriptionId);
}

