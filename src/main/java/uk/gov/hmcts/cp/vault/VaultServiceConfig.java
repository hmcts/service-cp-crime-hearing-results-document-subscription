package uk.gov.hmcts.cp.vault;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Getter
public class VaultServiceConfig {

    private final String vaultUri;

    public VaultServiceConfig(@Value("${vault.uri}") final String vaultUri) {
        log.info("Vault initialised with vaultUri:{}", vaultUri);
        this.vaultUri = vaultUri;
    }
}

