package uk.gov.hmcts.cp.vault;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@Getter
public class VaultServiceConfig {

    private final String vaultUri;
    private final UUID vaultClientId;

    public VaultServiceConfig(@Value("${vault.uri}") final String vaultUri,
                              @Value("${vault.client-id:}") final UUID vaultClientId) {
        log.info("Vault initialised with vaultUri:{}", vaultUri);
        log.info("Vault initialised with vaultClientId:{}", vaultClientId);
        this.vaultUri = vaultUri;
        this.vaultClientId = vaultClientId;
    }
}

