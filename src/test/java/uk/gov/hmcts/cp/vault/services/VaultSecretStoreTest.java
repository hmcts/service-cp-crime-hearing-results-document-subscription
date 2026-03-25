package uk.gov.hmcts.cp.vault.services;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.cp.subscription.integration.config.TestContainersInitialise;
import uk.gov.hmcts.cp.vault.services.store.VaultStartupService;

@SpringBootTest
@ContextConfiguration(initializers = TestContainersInitialise.class)
class VaultSecretStoreTest {

    @Autowired(required = false)
    VaultStartupService vaultStartupService;

    @Test
    void secrets_should_connect_and_be_listed() {
        if (vaultStartupService != null) {
            vaultStartupService.debugSecrets();
        }
        // no assertions - run locally with hmac.vault-enabled=true to check connection
    }
}