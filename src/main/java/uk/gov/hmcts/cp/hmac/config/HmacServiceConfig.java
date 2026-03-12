package uk.gov.hmcts.cp.hmac.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Getter
public class HmacServiceConfig {

    private final boolean vaultEnabled;
    private final String keyId;
    private final String secret;

    public HmacServiceConfig(
            @Value("${hmac.vault-enabled:false}") final boolean vaultEnabled,
            @Value("${hmac.key-id:}") final String keyId,
            @Value("${hmac.secret:}") final String secret) {
        log.info("Hmac initialised with enabled {}", vaultEnabled);
        log.info("Hmac initialised with keyId {}", keyId);
        this.vaultEnabled = vaultEnabled;
        this.keyId = keyId;
        this.secret = secret;
    }
}

