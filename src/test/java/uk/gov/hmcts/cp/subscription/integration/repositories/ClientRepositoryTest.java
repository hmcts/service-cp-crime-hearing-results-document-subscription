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
        // Given
        UUID clientId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        ClientEntity client = ClientEntity.builder()
                .id(clientId)
                .subscriptionId(subscriptionId)
                .callbackUrl("https://example.com/callback")
                .createdAt(now)
                .updatedAt(now)
                .build();

        // When
        ClientEntity saved = clientRepository.save(client);

        // Then
        assertThat(saved.getId()).isEqualTo(clientId);
        assertThat(saved.getSubscriptionId()).isEqualTo(subscriptionId);
        assertThat(saved.getCallbackUrl()).isEqualTo("https://example.com/callback");
        assertThat(saved.getCreatedAt()).isEqualTo(now);
        assertThat(saved.getUpdatedAt()).isEqualTo(now);
    }

    @Transactional
    @Test
    void findById_should_return_client_entity_when_exists() {
        // Given
        UUID clientId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        ClientEntity client = ClientEntity.builder()
                .id(clientId)
                .subscriptionId(subscriptionId)
                .callbackUrl("https://test.com/webhook")
                .createdAt(now)
                .updatedAt(now)
                .build();

        clientRepository.save(client);

        // When
        Optional<ClientEntity> found = clientRepository.findById(clientId);

        // Then
        assertThat(found.get().getId()).isEqualTo(clientId);
        assertThat(found.get().getSubscriptionId()).isEqualTo(subscriptionId);
        assertThat(found.get().getCallbackUrl()).isEqualTo("https://test.com/webhook");
    }

    @Transactional
    @Test
    void findById_should_return_empty_when_client_not_found() {
        // Given
        UUID nonExistentId = UUID.randomUUID();

        // When
        Optional<ClientEntity> found = clientRepository.findById(nonExistentId);

        // Then
        assertThat(found).isEmpty();
    }

    @Transactional
    @Test
    void save_should_update_existing_client_entity() {
        // Given
        UUID clientId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        OffsetDateTime originalTime = OffsetDateTime.now(ZoneOffset.UTC).minusHours(1);

        ClientEntity originalClient = ClientEntity.builder()
                .id(clientId)
                .subscriptionId(subscriptionId)
                .callbackUrl("https://original.com/callback")
                .createdAt(originalTime)
                .updatedAt(originalTime)
                .build();

        clientRepository.save(originalClient);

        OffsetDateTime updatedTime = OffsetDateTime.now(ZoneOffset.UTC);
        ClientEntity updatedClient = originalClient.toBuilder()
                .callbackUrl("https://updated.com/callback")
                .updatedAt(updatedTime)
                .build();

        // When
        ClientEntity saved = clientRepository.save(updatedClient);

        // Then
        assertThat(saved.getCallbackUrl()).isEqualTo("https://updated.com/callback");
        assertThat(saved.getUpdatedAt()).isEqualTo(updatedTime);
        assertThat(saved.getCreatedAt()).isEqualTo(originalTime);
    }
}
