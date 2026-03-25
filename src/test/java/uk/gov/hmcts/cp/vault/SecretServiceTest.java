package uk.gov.hmcts.cp.vault;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.cp.subscription.integration.config.TestContainersInitialise;

@SpringBootTest
@ContextConfiguration(initializers = TestContainersInitialise.class)
class SecretServiceTest {

    @Autowired
    SecretService secretService;

    @Test
    void secrets_should_connect_and_be_listed() {
        secretService.debugSecrets();

        // no assertions just run locally to check connection
    }
}