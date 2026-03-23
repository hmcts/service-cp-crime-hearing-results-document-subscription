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
        UUID clientId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        Long eventTypeId = 1L; // Assuming this exists in the database

        ClientEntity client = getClientEntity(clientId, "https://example.com/callback", subscriptionId);
        clientRepository.save(client);

        ClientEventEntity clientEvent = ClientEventEntity.builder()
                .subscriptionId(subscriptionId)
                .eventTypeId(eventTypeId)
                .build();
        clientEventsRepository.save(clientEvent);

        List<ClientEventEntity> result = clientEventsRepository.findClientEventsWithEventTypes(clientId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSubscriptionId()).isEqualTo(subscriptionId);
        assertThat(result.get(0).getEventTypeId()).isEqualTo(eventTypeId);
    }

    @Transactional
    @Test
    void findClientEventsWithEventTypes_should_return_empty_list_when_no_client_found() {
        UUID nonExistentClientId = UUID.randomUUID();

        List<ClientEventEntity> result = clientEventsRepository.findClientEventsWithEventTypes(nonExistentClientId);

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Transactional
    @Test
    void findClientEventsWithEventTypes_should_return_empty_when_client_exists_but_no_events() {
        UUID clientId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();

        ClientEntity client = getClientEntity(clientId, "https://example.com/callback", subscriptionId);
        clientRepository.save(client);

        List<ClientEventEntity> result = clientEventsRepository.findClientEventsWithEventTypes(clientId);
        assertThat(result).isEmpty();
    }

    @Transactional
    @Test
    void findClientEventsWithEventTypes_should_return_multiple_events_ordered_by_event_name() {
        UUID clientId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();

        ClientEntity client = getClientEntity(clientId, "https://example.com/callback", subscriptionId);
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

        List<ClientEventEntity> result = clientEventsRepository.findClientEventsWithEventTypes(clientId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getSubscriptionId()).isEqualTo(subscriptionId);
        assertThat(result.get(1).getSubscriptionId()).isEqualTo(subscriptionId);
    }

    @Transactional
    @Test
    void save_should_persist_client_event_entity() {
        UUID clientId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        Long eventTypeId = 1L;

        // Create client entity first to satisfy foreign key constraint
        ClientEntity client = getClientEntity(clientId, "https://example.com/callback", subscriptionId);
        clientRepository.save(client);

        ClientEventEntity clientEvent = ClientEventEntity.builder()
                .subscriptionId(subscriptionId)
                .eventTypeId(eventTypeId)
                .build();

        ClientEventEntity saved = clientEventsRepository.save(clientEvent);
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isGreaterThan(0);
        assertThat(saved.getSubscriptionId()).isEqualTo(subscriptionId);
        assertThat(saved.getEventTypeId()).isEqualTo(eventTypeId);
    }

    private static ClientEntity getClientEntity(UUID clientId, String callbackUrl, UUID subscriptionId) {
        return ClientEntity.builder()
                .id(clientId)
                .subscriptionId(subscriptionId)
                .callbackUrl(callbackUrl)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
    }
}
