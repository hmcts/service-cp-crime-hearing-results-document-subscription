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
import uk.gov.hmcts.cp.subscription.repositories.ClientEventsRepository;
import uk.gov.hmcts.cp.subscription.repositories.ClientRepository;
import uk.gov.hmcts.cp.subscription.repositories.EventTypeRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
        // Given
        EventTypeEntity eventType = EventTypeEntity.builder()
                .id(1L)
                .eventName("PRISON_COURT_REGISTER_GENERATED")
                .displayName("Prison court register")
                .category("REGISTER")
                .build();

        ClientSubscription clientSubscription = ClientSubscription.builder()
                .eventTypes(List.of(
                        EventType.PRISON_COURT_REGISTER_GENERATED
                ))
                .build();

        when(eventTypeRepository.findAll()).thenReturn(List.of(eventType));

        // When
        Set<EventTypeEntity> result = clientEventsService.getEventTypes(clientSubscription);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(eventType);
    }

   // TODO : write a test for multiple events and invalid event types in next commit AMP-197

    @Test
    void getEventTypes_should_handle_duplicate_event_types_in_subscription() {
        // Given
        EventTypeEntity eventType = EventTypeEntity.builder()
                .id(1L)
                .eventName("PRISON_COURT_REGISTER_GENERATED")
                .displayName("Prison court register")
                .category("REGISTER")
                .build();

        ClientSubscription clientSubscription = ClientSubscription.builder()
                .eventTypes(List.of(
                        EventType.PRISON_COURT_REGISTER_GENERATED,
                        EventType.PRISON_COURT_REGISTER_GENERATED
                ))
                .build();

        when(eventTypeRepository.findAll()).thenReturn(List.of(eventType));

        // When
        Set<EventTypeEntity> result = clientEventsService.getEventTypes(clientSubscription);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).contains(eventType);
    }

    @Test
    void saveClientInfo_should_save_client_and_event_entities() {
        // Given
        UUID clientId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();

        EventTypeEntity eventType = EventTypeEntity.builder()
                .id(1L)
                .eventName("PRISON_COURT_REGISTER_GENERATED")
                .displayName("Prison court register")
                .category("REGISTER")
                .build();

        ClientEventEntity clientEventEntity = ClientEventEntity.builder()
                .eventTypeId(1L)
                .subscriptionId(subscriptionId)
                .build();

        ClientSubscription clientSubscription = ClientSubscription.builder()
                .clientSubscriptionId(subscriptionId)
                .notificationEndpoint(NotificationEndpoint.builder()
                        .callbackUrl("https://example.com/callback")
                        .build())
                .eventTypes(List.of(
                        EventType.PRISON_COURT_REGISTER_GENERATED
                ))
                .build();

        when(eventTypeRepository.findAll()).thenReturn(List.of(eventType));

        ClientEntity clientEntity = ClientEntity.builder()
                .id(clientId)
                .subscriptionId(subscriptionId)
                .callbackUrl("https://example.com/callback")
                .build();

        when(clientMapper.mapToClientEntity(clockService, clientSubscription, clientId)).thenReturn(clientEntity);
        when(clientEventMapper.mapToClientEventEntityList(subscriptionId, Set.of(eventType))).thenReturn(List.of(clientEventEntity));

        // When
        clientEventsService.saveClientInfo(clientSubscription, clientId);

        // Then
        verify(eventTypeRepository).findAll();
        verify(clientRepository).save(clientEntity);
        verify(clientEventsRepository).save(any());
    }
}
