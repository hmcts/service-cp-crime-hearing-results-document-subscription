package uk.gov.hmcts.cp.subscription.integration.repositories;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.cp.subscription.entities.ClientEntity;
import uk.gov.hmcts.cp.subscription.entities.ClientEventEntity;
import uk.gov.hmcts.cp.subscription.integration.IntegrationTestBase;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ClientRepositoryTest extends IntegrationTestBase {

    final UUID clientId1 = UUID.randomUUID();
    final UUID clientId2 = UUID.randomUUID();
    final UUID subscriptionId1 = UUID.randomUUID();
    final UUID subscriptionId2 = UUID.randomUUID();
    final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

    final ClientEventEntity event1 = getClientEventEntity(subscriptionId1, 1L);
    final ClientEventEntity event2 = getClientEventEntity(subscriptionId2, 2L);
    final ClientEntity client1 = getClientEntity(clientId1, subscriptionId1, "https://test.com/webhook", now);
    final ClientEntity client2 = getClientEntity(clientId2, subscriptionId2, "https://test.com/webhook", now);


    @Transactional
    @Test
    void findById_should_return_client_entity_when_exists() {
        clientRepository.save(client1);

        Optional<ClientEntity> found = clientRepository.findByIdAndSubscriptionId(clientId1, subscriptionId1);
        assertThat(found.isPresent()).isTrue();
        assertThat(found.get().getId()).isEqualTo(clientId1);
        assertThat(found.get().getSubscriptionId()).isEqualTo(subscriptionId1);
        assertThat(found.get().getCallbackUrl()).isEqualTo("https://test.com/webhook");
    }

    @Transactional
    @Test
    void findById_should_return_empty_when_client_not_found() {
        UUID nonExistentId = UUID.randomUUID();
        clientRepository.save(client1);

        Optional<ClientEntity> found = clientRepository.findByIdAndSubscriptionId(nonExistentId, subscriptionId1);
        assertThat(found).isEmpty();
    }

    @Test
    void get_client_for_event_type_should_return_client_list() {
        saveClientAndEventInfoInDb(client1, List.of(event1));
        saveClientAndEventInfoInDb(client2, List.of(event2));
        List<ClientEntity> clients = clientRepository.findClientsByEventType("PRISON_COURT_REGISTER_GENERATED");
        assertThat(clients).hasSize(1);
        assertThat(clients.getFirst()).isEqualTo(client1);
    }

    private void saveClientAndEventInfoInDb(ClientEntity client, List<ClientEventEntity> events) {
        clientRepository.save(client);
        clientEventRepository.saveAll(events);
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

    private static ClientEventEntity getClientEventEntity(UUID subscriptionId, long eventId) {
        return ClientEventEntity.builder()
                .subscriptionId(subscriptionId)
                .eventTypeId(eventId)
                .build();
    }
}