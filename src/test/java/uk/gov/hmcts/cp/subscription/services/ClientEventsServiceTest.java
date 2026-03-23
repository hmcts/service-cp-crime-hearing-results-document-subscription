package uk.gov.hmcts.cp.subscription.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cp.openapi.model.ClientSubscription;
import uk.gov.hmcts.cp.openapi.model.EventType;
import uk.gov.hmcts.cp.openapi.model.NotificationEndpoint;
import uk.gov.hmcts.cp.subscription.entities.ClientEntity;
import uk.gov.hmcts.cp.subscription.entities.ClientEventEntity;
import uk.gov.hmcts.cp.subscription.entities.EventTypeEntity;
import uk.gov.hmcts.cp.subscription.mappers.ClientEventMapper;
import uk.gov.hmcts.cp.subscription.mappers.ClientMapper;
import uk.gov.hmcts.cp.subscription.model.EntityEventType;
import uk.gov.hmcts.cp.subscription.repositories.ClientEventsRepository;
import uk.gov.hmcts.cp.subscription.repositories.ClientRepository;
import uk.gov.hmcts.cp.subscription.repositories.EventTypeRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientEventsServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ClientEventsRepository clientEventsRepository;

    @Mock
    private EventTypeRepository eventTypeRepository;

    @Mock
    private ClientMapper clientMapper;

    @Mock
    private ClientEventMapper clientEventMapper;

    @Mock
    private ClockService clockService;

    @InjectMocks
    private ClientEventsService clientEventsService;

    @Test
    void getEventTypes_should_return_event_type_entities_for_valid_event_types() {
        EventTypeEntity eventType = getEventTypeEntity();

        ClientSubscription clientSubscription = getClientSubscription(UUID.randomUUID(), List.of(
                EventType.PRISON_COURT_REGISTER_GENERATED
        ));

        when(eventTypeRepository.findAll()).thenReturn(List.of(eventType));

        Set<EventTypeEntity> result = clientEventsService.getEventTypes(clientSubscription);
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(eventType);
    }

    // TODO : write a test for multiple events and invalid event types in next commit AMP-197


    @Test
    void getEventTypes_should_throw_for_unknown_event_type() {
        ClientSubscription clientSubscription = getClientSubscription(UUID.randomUUID(), List.of(
                EventType.PRISON_COURT_REGISTER_GENERATED
        ));

        when(eventTypeRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> clientEventsService.getEventTypes(clientSubscription))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid event type");
    }

    @Test
    void getEventTypes_should_handle_duplicate_event_types_in_subscription() {
        EventTypeEntity eventType = getEventTypeEntity();

        ClientSubscription clientSubscription = getClientSubscription(UUID.randomUUID(), List.of(
                        EventType.PRISON_COURT_REGISTER_GENERATED,
                        EventType.PRISON_COURT_REGISTER_GENERATED
                ));

        when(eventTypeRepository.findAll()).thenReturn(List.of(eventType));

        Set<EventTypeEntity> result = clientEventsService.getEventTypes(clientSubscription);

        assertThat(result).hasSize(1);
        assertThat(result).contains(eventType);
    }

    @Test
    void saveClientInfo_should_save_client_and_event_entities() {
        UUID clientId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();

        EventTypeEntity eventType = getEventTypeEntity();

        ClientEventEntity clientEventEntity = ClientEventEntity.builder()
                .eventTypeId(1L)
                .subscriptionId(subscriptionId)
                .build();

        ClientSubscription clientSubscription = getClientSubscription(subscriptionId, List.of(
                        EventType.PRISON_COURT_REGISTER_GENERATED
                ));

        when(eventTypeRepository.findAll()).thenReturn(List.of(eventType));

        ClientEntity clientEntity = ClientEntity.builder()
                .id(clientId)
                .subscriptionId(subscriptionId)
                .callbackUrl("https://example.com/callback")
                .build();

        when(clientMapper.mapToClientEntity(clockService, clientSubscription, clientId)).thenReturn(clientEntity);
        when(clientEventMapper.mapToClientEventEntityList(subscriptionId, Set.of(eventType))).thenReturn(List.of(clientEventEntity));

        clientEventsService.saveClientInfo(clientSubscription, clientId);
        verify(eventTypeRepository).findAll();
        verify(clientRepository).save(clientEntity);
        verify(clientEventsRepository).saveAll(any());
    }

    @Test
    void updateClientInfo_should_update_callback_url_and_replace_events() {
        UUID clientId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();

        EventTypeEntity eventType = getEventTypeEntity();
        ClientEventEntity clientEventEntity = ClientEventEntity.builder()
                .eventTypeId(1L)
                .subscriptionId(subscriptionId)
                .build();
        ClientSubscription clientSubscription = getClientSubscription(subscriptionId, List.of(
                EventType.PRISON_COURT_REGISTER_GENERATED
        ));

        when(eventTypeRepository.findAll()).thenReturn(List.of(eventType));
        when(clientEventMapper.mapToClientEventEntityList(subscriptionId, Set.of(eventType))).thenReturn(List.of(clientEventEntity));

        clientEventsService.updateClientInfo(clientSubscription, clientId);

        verify(clientRepository).updateCallbackUrl(eq(clientId), eq("https://example.com/callback"), any());
        verify(clientEventsRepository).deleteBySubscriptionId(subscriptionId);
        verify(clientEventsRepository).saveAll(any());
    }

    @Test
    void deleteClientInfo_should_delete_events_and_client() {
        UUID clientSubscriptionId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();

        clientEventsService.deleteClientInfo(clientSubscriptionId, clientId);

        verify(clientEventsRepository).deleteBySubscriptionId(clientSubscriptionId);
        verify(clientRepository).deleteById(clientId);
    }

    @Test
    void hasAccess_should_return_true_when_matching_event_exists() {
        UUID clientSubscriptionId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();

        when(clientEventsRepository.countBySubscriptionAndClientAndEventName(
                clientSubscriptionId, clientId, EntityEventType.PRISON_COURT_REGISTER_GENERATED.name())).thenReturn(1L);

        assertThat(clientEventsService.hasAccess(clientSubscriptionId, clientId, EntityEventType.PRISON_COURT_REGISTER_GENERATED)).isTrue();
    }

    @Test
    void hasAccess_should_return_false_when_no_matching_event() {
        UUID clientSubscriptionId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();

        when(clientEventsRepository.countBySubscriptionAndClientAndEventName(
                clientSubscriptionId, clientId, EntityEventType.PRISON_COURT_REGISTER_GENERATED.name())).thenReturn(0L);

        assertThat(clientEventsService.hasAccess(clientSubscriptionId, clientId, EntityEventType.PRISON_COURT_REGISTER_GENERATED)).isFalse();
    }

    private static EventTypeEntity getEventTypeEntity() {
        return EventTypeEntity.builder()
                .id(1L)
                .eventName("PRISON_COURT_REGISTER_GENERATED")
                .displayName("Prison court register")
                .category("REGISTER")
                .build();
    }

    private static ClientSubscription getClientSubscription(UUID subscriptionId, List<EventType> eventTypes) {
        return ClientSubscription.builder()
                .clientSubscriptionId(subscriptionId)
                .notificationEndpoint(NotificationEndpoint.builder()
                        .callbackUrl("https://example.com/callback")
                        .build())
                .eventTypes(eventTypes)
                .build();
    }
}
