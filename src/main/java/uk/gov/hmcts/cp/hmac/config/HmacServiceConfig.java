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

    public HmacServiceConfig(@Value("${hmac.vault-enabled:false}") final boolean vaultEnabled) {
        log.info("Hmac initialised with vaultEnabled:{}", vaultEnabled);
        this.vaultEnabled = vaultEnabled;
    }
}

