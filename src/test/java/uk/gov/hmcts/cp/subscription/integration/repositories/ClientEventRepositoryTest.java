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

class ClientEventRepositoryTest extends IntegrationTestBase {

    @Transactional
    @Test
    void findClientEventsWithEventTypes_should_return_events_ordered_by_event_name() {
        UUID clientId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();

        ClientEntity client = getClientEntity(clientId, "https://example.com/callback", subscriptionId);
        clientRepository.save(client);

        ClientEventEntity event1 = getClientEventEntity(subscriptionId, 1L);
        ClientEventEntity event2 = getClientEventEntity(subscriptionId, 2L);

        clientEventRepository.save(event1);
        clientEventRepository.save(event2);

        List<String> result = clientEventRepository.findEventNamesForClient(clientId, subscriptionId);

        assertThat(result).hasSize(2);
        assertThat(result.getFirst()).isEqualTo("PRISON_COURT_REGISTER_GENERATED");
        assertThat(result.getLast()).isEqualTo("WEE_Layout5");
    }

    @Transactional
    @Test
    void countByClientSubscriptionAndEventName_should_return_count_if_event_exists() {
        UUID clientId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();

        ClientEntity client = getClientEntity(clientId, "https://example.com/callback", subscriptionId);
        clientRepository.save(client);

        ClientEventEntity event = getClientEventEntity(subscriptionId, 1L);

        clientEventRepository.save(event);

        long result = clientEventRepository.countByClientSubscriptionAndEventName(clientId, subscriptionId, "PRISON_COURT_REGISTER_GENERATED");
        assertThat(result).isEqualTo(1);
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

        ClientEventEntity clientEvent = getClientEventEntity(subscriptionId, 1L);

        ClientEventEntity saved = clientEventRepository.save(clientEvent);
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isGreaterThan(0);
        assertThat(saved.getSubscriptionId()).isEqualTo(subscriptionId);
        assertThat(saved.getEventTypeId()).isEqualTo(eventTypeId);
    }

    @Transactional
    @Test
    void deleteBySubscriptionId_should_delete_client_event_entity() {
        UUID subscriptionId1 = UUID.randomUUID();
        UUID subscriptionId2 = UUID.randomUUID();
        UUID clientId1 = UUID.randomUUID();
        UUID clientId2 = UUID.randomUUID();

        ClientEntity client1 = getClientEntity(clientId1, "https://example.com/callback", subscriptionId1);
        clientRepository.save(client1);

        ClientEntity client2 = getClientEntity(clientId2, "https://example.com/callback", subscriptionId2);
        clientRepository.save(client2);

        ClientEventEntity clientEvent1 = getClientEventEntity(subscriptionId1, 1L);
        ClientEventEntity clientEvent2 = getClientEventEntity(subscriptionId2, 2L);

        clientEventRepository.save(clientEvent1);
        clientEventRepository.save(clientEvent2);

        clientEventRepository.deleteBySubscriptionId(subscriptionId1);

        List<ClientEventEntity> result = clientEventRepository.findAll();
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.getFirst().getSubscriptionId()).isEqualTo(subscriptionId2);
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

    private static ClientEventEntity getClientEventEntity(UUID subscriptionId, long eventId) {
        return ClientEventEntity.builder()
                .subscriptionId(subscriptionId)
                .eventTypeId(eventId)
                .build();
    }
}