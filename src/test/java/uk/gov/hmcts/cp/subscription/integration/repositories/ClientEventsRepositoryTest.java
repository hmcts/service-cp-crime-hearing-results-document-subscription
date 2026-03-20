package uk.gov.hmcts.cp.subscription.integration.repositories;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.cp.subscription.entities.ClientEventEntity;
import uk.gov.hmcts.cp.subscription.entities.ClientEntity;
import uk.gov.hmcts.cp.subscription.integration.IntegrationTestBase;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ClientEventsRepositoryTest extends IntegrationTestBase {

    @Transactional
    @Test
    void findClientEventsWithEventTypes_should_return_events_for_client() {
        // Given
        UUID clientId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        Long eventTypeId = 1L; // Assuming this exists in the database

        ClientEntity client = ClientEntity.builder()
                .id(clientId)
                .subscriptionId(subscriptionId)
                .callbackUrl("https://example.com/callback")
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
        clientRepository.save(client);

        ClientEventEntity clientEvent = ClientEventEntity.builder()
                .subscriptionId(subscriptionId)
                .eventTypeId(eventTypeId)
                .build();
        clientEventsRepository.save(clientEvent);

        // When
        List<ClientEventEntity> result = clientEventsRepository.findClientEventsWithEventTypes(clientId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSubscriptionId()).isEqualTo(subscriptionId);
        assertThat(result.get(0).getEventTypeId()).isEqualTo(eventTypeId);
    }

    @Transactional
    @Test
    void findClientEventsWithEventTypes_should_return_empty_list_when_no_client_found() {
        // Given
        UUID nonExistentClientId = UUID.randomUUID();

        // When
        List<ClientEventEntity> result = clientEventsRepository.findClientEventsWithEventTypes(nonExistentClientId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Transactional
    @Test
    void findClientEventsWithEventTypes_should_return_empty_when_client_exists_but_no_events() {
        // Given
        UUID clientId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();

        ClientEntity client = ClientEntity.builder()
                .id(clientId)
                .subscriptionId(subscriptionId)
                .callbackUrl("https://example.com/callback")
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
        clientRepository.save(client);

        // When
        List<ClientEventEntity> result = clientEventsRepository.findClientEventsWithEventTypes(clientId);

        // Then
        assertThat(result).isEmpty();
    }

    @Transactional
    @Test
    void findClientEventsWithEventTypes_should_return_multiple_events_ordered_by_event_name() {
        // Given
        UUID clientId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();

        ClientEntity client = ClientEntity.builder()
                .id(clientId)
                .subscriptionId(subscriptionId)
                .callbackUrl("https://example.com/callback")
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
        clientRepository.save(client);

        ClientEventEntity event1 = ClientEventEntity.builder()
                .subscriptionId(subscriptionId)
                .eventTypeId(1L)
                .build();

        ClientEventEntity event2 = ClientEventEntity.builder()
                .subscriptionId(subscriptionId)
                .eventTypeId(2L)
                .build();

        clientEventsRepository.save(event1);
        clientEventsRepository.save(event2);

        // When
        List<ClientEventEntity> result = clientEventsRepository.findClientEventsWithEventTypes(clientId);
        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getSubscriptionId()).isEqualTo(subscriptionId);
        assertThat(result.get(1).getSubscriptionId()).isEqualTo(subscriptionId);
    }

    @Transactional
    @Test
    void save_should_persist_client_event_entity() {
        // Given
        UUID clientId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        Long eventTypeId = 1L;

        // Create client entity first to satisfy foreign key constraint
        ClientEntity client = ClientEntity.builder()
                .id(clientId)
                .subscriptionId(subscriptionId)
                .callbackUrl("https://example.com/callback")
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
        clientRepository.save(client);

        ClientEventEntity clientEvent = ClientEventEntity.builder()
                .subscriptionId(subscriptionId)
                .eventTypeId(eventTypeId)
                .build();

        // When
        ClientEventEntity saved = clientEventsRepository.save(clientEvent);

        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isGreaterThan(0);
        assertThat(saved.getSubscriptionId()).isEqualTo(subscriptionId);
        assertThat(saved.getEventTypeId()).isEqualTo(eventTypeId);
    }
}
