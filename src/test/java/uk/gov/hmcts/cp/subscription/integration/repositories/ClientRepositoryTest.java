package uk.gov.hmcts.cp.subscription.integration.repositories;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.cp.subscription.entities.ClientEntity;
import uk.gov.hmcts.cp.subscription.integration.IntegrationTestBase;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ClientRepositoryTest extends IntegrationTestBase {

    @Transactional
    @Test
    void save_should_persist_client_entity() {
        UUID clientId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        ClientEntity client = getClientEntity(clientId, subscriptionId, "https://example.com/callback", now);

        ClientEntity saved = clientRepository.save(client);
        assertThat(saved.getId()).isEqualTo(clientId);
        assertThat(saved.getSubscriptionId()).isEqualTo(subscriptionId);
        assertThat(saved.getCallbackUrl()).isEqualTo("https://example.com/callback");
        assertThat(saved.getCreatedAt()).isEqualTo(now);
        assertThat(saved.getUpdatedAt()).isEqualTo(now);
    }

    @Transactional
    @Test
    void findById_should_return_client_entity_when_exists() {
        UUID clientId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        ClientEntity client = getClientEntity(clientId, subscriptionId, "https://test.com/webhook", now);

        clientRepository.save(client);

        Optional<ClientEntity> found = clientRepository.findById(clientId);
        assertThat(found.isPresent()).isTrue();
        assertThat(found.get().getId()).isEqualTo(clientId);
        assertThat(found.get().getSubscriptionId()).isEqualTo(subscriptionId);
        assertThat(found.get().getCallbackUrl()).isEqualTo("https://test.com/webhook");
    }

    @Transactional
    @Test
    void findById_should_return_empty_when_client_not_found() {
        UUID nonExistentId = UUID.randomUUID();
        Optional<ClientEntity> found = clientRepository.findById(nonExistentId);
        assertThat(found).isEmpty();
    }

    @Transactional
    @Test
    void save_should_update_existing_client_entity() {
        UUID clientId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        OffsetDateTime originalTime = OffsetDateTime.now(ZoneOffset.UTC).minusHours(1);

        ClientEntity originalClient = getClientEntity(clientId, subscriptionId, "https://original.com/callback", originalTime);

        clientRepository.save(originalClient);

        OffsetDateTime updatedTime = OffsetDateTime.now(ZoneOffset.UTC);
        ClientEntity updatedClient = originalClient.toBuilder()
                .callbackUrl("https://updated.com/callback")
                .updatedAt(updatedTime)
                .build();

        ClientEntity saved = clientRepository.save(updatedClient);
        assertThat(saved.getCallbackUrl()).isEqualTo("https://updated.com/callback");
        assertThat(saved.getUpdatedAt()).isEqualTo(updatedTime);
        assertThat(saved.getCreatedAt()).isEqualTo(originalTime);
    }

    private static ClientEntity getClientEntity(UUID clientId, UUID subscriptionId, String callbackUrl, OffsetDateTime originalTime) {
        return ClientEntity.builder()
                .id(clientId)
                .subscriptionId(subscriptionId)
                .callbackUrl(callbackUrl)
                .createdAt(originalTime)
                .updatedAt(originalTime)
                .build();
    }
}
