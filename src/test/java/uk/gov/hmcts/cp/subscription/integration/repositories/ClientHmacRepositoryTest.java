package uk.gov.hmcts.cp.subscription.integration.repositories;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.cp.subscription.entities.ClientHmacEntity;
import uk.gov.hmcts.cp.subscription.integration.IntegrationTestBase;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ClientHmacRepositoryTest extends IntegrationTestBase {

    UUID clientId = UUID.randomUUID();
    UUID subscriptionId = UUID.randomUUID();
    String keyId = "kid-" + UUID.randomUUID();
    ClientHmacEntity clientHmac = ClientHmacEntity.builder()
            .subscriptionId(subscriptionId)
            .keyId(keyId)
            .build();

    @BeforeEach
    void beforeEach(){
        super.clearAllTables();
    }

    @Test
    void client_hmac_table_should_write_and_read() {
        insertClient(clientId, subscriptionId);
        clientHmacRepository.save(clientHmac);

        ClientHmacEntity retrieved = clientHmacRepository.findBySubscriptionId(subscriptionId).get();
        assertThat(retrieved.getKeyId()).isEqualTo(keyId);
    }

    @Test
    @Transactional
    void delete_should_delete() {
        insertClient(clientId, subscriptionId);
        clientHmacRepository.save(clientHmac);
        assertThat(clientHmacRepository.findAll()).hasSize(1);
        clientHmacRepository.deleteAllBySubscriptionId(subscriptionId);
        assertThat(clientHmacRepository.findAll()).hasSize(0);
    }
}
