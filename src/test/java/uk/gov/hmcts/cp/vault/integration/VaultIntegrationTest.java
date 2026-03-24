package uk.gov.hmcts.cp.vault.integration;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
public class VaultIntegrationTest {

    @Test
    void vault_connection_should_try_on_startup() {
        log.info("Vault test");
    }
}
