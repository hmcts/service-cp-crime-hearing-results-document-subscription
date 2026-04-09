package uk.gov.hmcts.cp.subscription.unit.services;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.cp.openapi.model.ClientSubscriptionRequest;
import uk.gov.hmcts.cp.openapi.model.NotificationEndpoint;
import uk.gov.hmcts.cp.subscription.entities.ClientEntity;
import uk.gov.hmcts.cp.subscription.entities.EventTypeEntity;
import uk.gov.hmcts.cp.subscription.repositories.ClientRepository;
import uk.gov.hmcts.cp.subscription.repositories.EventTypeRepository;
import uk.gov.hmcts.cp.subscription.services.SubscriptionValidationService;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionValidationServiceTest {

    @Mock
    ClientRepository clientRepository;
    @Mock
    EventTypeRepository eventTypeRepository;

    @InjectMocks
    SubscriptionValidationService validationService;

    UUID clientId = UUID.fromString("11111111-2222-3333-4444-555555555555");
    UUID subscriptionId = UUID.fromString("2ca16eb5-3998-4bb7-adce-4bb9b3b7223c");
    ClientEntity clientEntity = ClientEntity.builder()
            .clientId(clientId)
            .subscriptionId(subscriptionId)
            .build();
    EventTypeEntity validEventType = EventTypeEntity.builder()
            .eventName("PRISON_COURT_REGISTER_GENERATED")
            .id(1L)
            .build();
    

    @Test
    void validateClientDoesNotExist_should_pass_when_no_existing_client() {
        when(clientRepository.findById(clientId)).thenReturn(Optional.empty());
        
        validationService.validateClientDoesNotExist(clientId);
        verify(clientRepository).findById(clientId);
    }

    @Test
    void validateClientDoesNotExist_should_throw_conflict_when_client_already_exists() {
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(clientEntity));

        assertThatThrownBy(() -> validationService.validateClientDoesNotExist(clientId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> {
                    ResponseStatusException ex = (ResponseStatusException) e;
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(ex.getReason()).isEqualTo("subscription already exist with " + subscriptionId);
                });
    }

    @Test
    void validateAndFetchClient_should_return_client_when_found() {
        when(clientRepository.findByClientIdAndSubscriptionId(clientId, subscriptionId)).thenReturn(Optional.of(clientEntity));

        ClientEntity result = validationService.validateAndFetchClient(clientId, subscriptionId);
        assertThat(result).isEqualTo(clientEntity);
    }

    @Test
    void validateAndFetchClient_should_throw_entity_not_found_when_client_missing() {
        when(clientRepository.findByClientIdAndSubscriptionId(clientId, subscriptionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> validationService.validateAndFetchClient(clientId, subscriptionId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Client not found for the provided clientId and subscriptionId");
    }

    @Test
    void validateAndFetchEventIds_should_return_event_ids_when_all_event_types_valid() {
        ClientSubscriptionRequest request = ClientSubscriptionRequest.builder()
                .notificationEndpoint(NotificationEndpoint.builder().callbackUrl("https://example.com/callback").build())
                .eventTypes(List.of("PRISON_COURT_REGISTER_GENERATED"))
                .build();
        when(eventTypeRepository.findByEventNameIn(Set.of("PRISON_COURT_REGISTER_GENERATED")))
                .thenReturn(List.of(validEventType));

        List<Long> result = validationService.validateAndFetchEventIds(request);
        assertThat(result).containsExactly(1L);
    }

    @Test
    void validateAndFetchEventIds_should_throw_when_one_event_type_is_invalid() {
        ClientSubscriptionRequest request = ClientSubscriptionRequest.builder()
                .notificationEndpoint(NotificationEndpoint.builder().callbackUrl("https://example.com/callback").build())
                .eventTypes(List.of("PRISON_COURT_REGISTER_GENERATED", "BAD"))
                .build();
        when(eventTypeRepository.findByEventNameIn(Set.of("PRISON_COURT_REGISTER_GENERATED", "BAD")))
                .thenReturn(List.of(validEventType));

        assertThatThrownBy(() -> validationService.validateAndFetchEventIds(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid event type(s): BAD");
    }

    @Test
    void validateAndFetchEventIds_should_throw_listing_all_invalid_event_types() {
        ClientSubscriptionRequest request = ClientSubscriptionRequest.builder()
                .notificationEndpoint(NotificationEndpoint.builder().callbackUrl("https://example.com/callback").build())
                .eventTypes(List.of("INVALID_TYPE", "BAD", "UNKNOWN_TYPE"))
                .build();
        when(eventTypeRepository.findByEventNameIn(Set.of("INVALID_TYPE", "BAD", "UNKNOWN_TYPE")))
                .thenReturn(List.of());

        assertThatThrownBy(() -> validationService.validateAndFetchEventIds(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContainingAll("Invalid event type(s):", "INVALID_TYPE", "BAD", "UNKNOWN_TYPE");
    }

    @Test
    void validateAndFetchEventIds_should_throw_when_all_event_types_are_invalid() {
        ClientSubscriptionRequest request = ClientSubscriptionRequest.builder()
                .notificationEndpoint(NotificationEndpoint.builder().callbackUrl("https://example.com/callback").build())
                .eventTypes(List.of("BAD"))
                .build();
        when(eventTypeRepository.findByEventNameIn(Set.of("BAD"))).thenReturn(List.of());

        assertThatThrownBy(() -> validationService.validateAndFetchEventIds(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid event type(s): BAD");
    }
}