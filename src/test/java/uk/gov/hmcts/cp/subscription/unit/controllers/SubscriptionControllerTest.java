package uk.gov.hmcts.cp.subscription.unit.controllers;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import uk.gov.hmcts.cp.openapi.model.ClientSubscription;
import uk.gov.hmcts.cp.openapi.model.ClientSubscriptionRequest;
import uk.gov.hmcts.cp.openapi.model.NotificationEndpoint;
import uk.gov.hmcts.cp.subscription.controllers.SubscriptionController;
import uk.gov.hmcts.cp.filters.ClientIdResolutionFilter;
import uk.gov.hmcts.cp.subscription.entities.ClientEntity;
import uk.gov.hmcts.cp.subscription.services.EventTypeService;
import uk.gov.hmcts.cp.subscription.services.SubscriptionService;
import uk.gov.hmcts.cp.subscription.services.SubscriptionValidationService;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionControllerTest {

    @Mock
    SubscriptionService subscriptionService;

    @Mock
    EventTypeService eventTypeService;

    @Mock
    SubscriptionValidationService subscriptionValidationService;

    @InjectMocks
    SubscriptionController subscriptionController;

    private static final UUID TEST_CLIENT_UUID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @BeforeEach
    void setMdcClientId() {
        MDC.put(ClientIdResolutionFilter.MDC_CLIENT_ID, TEST_CLIENT_UUID.toString());
    }

    @AfterEach
    void clearMdcClientId() {
        MDC.remove(ClientIdResolutionFilter.MDC_CLIENT_ID);
    }

    ClientSubscriptionRequest createRequest = ClientSubscriptionRequest.builder()
            .notificationEndpoint(NotificationEndpoint.builder().callbackUrl("https://example.com/callback").build())
            .eventTypes(List.of("PRISON_COURT_REGISTER_GENERATED"))
            .build();
    ClientSubscriptionRequest updateRequest = ClientSubscriptionRequest.builder()
            .notificationEndpoint(NotificationEndpoint.builder().callbackUrl("https://example.com/callback").build())
            .eventTypes(List.of("PRISON_COURT_REGISTER_GENERATED"))
            .build();
    UUID subscriptionId = UUID.randomUUID();
    ClientEntity clientEntity = ClientEntity.builder()
            .clientId(TEST_CLIENT_UUID)
            .subscriptionId(subscriptionId)
            .build();

    @Test
    void create_controller_should_call_service() {
        ClientSubscription response = ClientSubscription.builder().clientSubscriptionId(subscriptionId).build();
        when(subscriptionValidationService.validateAndFetchEventIds(createRequest)).thenReturn(List.of(1L));
        when(subscriptionService.createClientSubscription(createRequest, TEST_CLIENT_UUID, List.of(1L))).thenReturn(response);

        var result = subscriptionController.createClientSubscription(createRequest, null);

        verify(subscriptionValidationService).validateClientDoesNotExist(TEST_CLIENT_UUID);
        verify(subscriptionValidationService).validateAndFetchEventIds(createRequest);
        verify(subscriptionService).createClientSubscription(createRequest, TEST_CLIENT_UUID, List.of(1L));
        assertThat(result.getStatusCode().value()).isEqualTo(201);
        assertThat(result.getBody()).isEqualTo(response);
    }

    @Test
    void create_controller_should_throw_conflict_when_client_already_exists() {
        ResponseStatusException conflict = new ResponseStatusException(HttpStatus.CONFLICT,
                "subscription already exist with " + subscriptionId);
        doThrow(conflict)
                .when(subscriptionValidationService).validateClientDoesNotExist(TEST_CLIENT_UUID);

        assertThatThrownBy(() -> subscriptionController.createClientSubscription(createRequest, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verify(subscriptionValidationService).validateClientDoesNotExist(TEST_CLIENT_UUID);
        verify(subscriptionValidationService, never()).validateAndFetchEventIds(any());
        verify(subscriptionService, never()).createClientSubscription(any(), any(), any());
    }

    @Test
    void create_controller_should_throw_when_event_types_are_invalid() {
        doThrow(new IllegalArgumentException("Invalid event type(s): BAD"))
                .when(subscriptionValidationService).validateAndFetchEventIds(createRequest);

        assertThatThrownBy(() -> subscriptionController.createClientSubscription(createRequest, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid event type(s): BAD");

        verify(subscriptionValidationService).validateClientDoesNotExist(TEST_CLIENT_UUID);
        verify(subscriptionValidationService).validateAndFetchEventIds(createRequest);
        verify(subscriptionService, never()).createClientSubscription(any(), any(), any());
    }

    @Test
    void update_controller_should_call_service() {
        ClientSubscription response = ClientSubscription.builder().clientSubscriptionId(subscriptionId).build();
        when(subscriptionValidationService.validateAndFetchClient(TEST_CLIENT_UUID, subscriptionId)).thenReturn(clientEntity);
        when(subscriptionValidationService.validateAndFetchEventIds(updateRequest)).thenReturn(List.of(1L));
        when(subscriptionService.updateClientSubscription(updateRequest, clientEntity, List.of(1L))).thenReturn(response);

        var result = subscriptionController.updateClientSubscription(subscriptionId, updateRequest, null);

        verify(subscriptionValidationService).validateAndFetchClient(TEST_CLIENT_UUID, subscriptionId);
        verify(subscriptionValidationService).validateAndFetchEventIds(updateRequest);
        verify(subscriptionService).updateClientSubscription(updateRequest, clientEntity, List.of(1L));
        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isEqualTo(response);
    }

    @Test
    void update_controller_should_throw_when_client_not_found() {
        EntityNotFoundException notFound = new EntityNotFoundException("Client not found for the provided clientId and subscriptionId");
        doThrow(notFound)
                .when(subscriptionValidationService).validateAndFetchClient(TEST_CLIENT_UUID, subscriptionId);

        assertThatThrownBy(() -> subscriptionController.updateClientSubscription(subscriptionId, updateRequest, null))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
                .hasMessage("Client not found for the provided clientId and subscriptionId");

        verify(subscriptionValidationService).validateAndFetchClient(TEST_CLIENT_UUID, subscriptionId);
        verify(subscriptionValidationService, never()).validateAndFetchEventIds(any());
        verify(subscriptionService, never()).updateClientSubscription(any(), any(), any());
    }

    @Test
    void update_controller_should_throw_when_event_types_are_invalid() {
        when(subscriptionValidationService.validateAndFetchClient(TEST_CLIENT_UUID, subscriptionId)).thenReturn(clientEntity);
        doThrow(new IllegalArgumentException("Invalid event type(s): BAD"))
                .when(subscriptionValidationService).validateAndFetchEventIds(updateRequest);

        assertThatThrownBy(() -> subscriptionController.updateClientSubscription(subscriptionId, updateRequest, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid event type(s): BAD");

        verify(subscriptionValidationService).validateAndFetchClient(TEST_CLIENT_UUID, subscriptionId);
        verify(subscriptionValidationService).validateAndFetchEventIds(updateRequest);
        verify(subscriptionService, never()).updateClientSubscription(any(), any(), any());
    }

    @Test
    void get_controller_should_call_service() {
        ClientSubscription response = ClientSubscription.builder().clientSubscriptionId(subscriptionId).build();
        when(subscriptionValidationService.validateAndFetchClient(TEST_CLIENT_UUID, subscriptionId)).thenReturn(clientEntity);
        when(subscriptionService.getClientSubscription(clientEntity)).thenReturn(response);

        var result = subscriptionController.getClientSubscription(subscriptionId, null);

        verify(subscriptionValidationService).validateAndFetchClient(TEST_CLIENT_UUID, subscriptionId);
        verify(subscriptionService).getClientSubscription(clientEntity);
        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isEqualTo(response);
    }

    @Test
    void get_controller_should_throw_when_client_not_found() {
        doThrow(new EntityNotFoundException("Client not found for the provided clientId and subscriptionId"))
                .when(subscriptionValidationService).validateAndFetchClient(TEST_CLIENT_UUID, subscriptionId);

        assertThatThrownBy(() -> subscriptionController.getClientSubscription(subscriptionId, null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Client not found for the provided clientId and subscriptionId");

        verify(subscriptionValidationService).validateAndFetchClient(TEST_CLIENT_UUID, subscriptionId);
        verify(subscriptionService, never()).getClientSubscription(any());
    }

    @Test
    void delete_controller_should_call_service() {
        when(subscriptionValidationService.validateAndFetchClient(TEST_CLIENT_UUID, subscriptionId)).thenReturn(clientEntity);

        var result = subscriptionController.deleteClientSubscription(subscriptionId, null);

        verify(subscriptionValidationService).validateAndFetchClient(TEST_CLIENT_UUID, subscriptionId);
        verify(subscriptionService).deleteClientSubscription(clientEntity);
        assertThat(result.getStatusCode().value()).isEqualTo(204);
    }

    @Test
    void delete_controller_should_throw_when_client_not_found() {
        doThrow(new EntityNotFoundException("Client not found for the provided clientId and subscriptionId"))
                .when(subscriptionValidationService).validateAndFetchClient(TEST_CLIENT_UUID, subscriptionId);

        assertThatThrownBy(() -> subscriptionController.deleteClientSubscription(subscriptionId, null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Client not found for the provided clientId and subscriptionId");

        verify(subscriptionValidationService).validateAndFetchClient(TEST_CLIENT_UUID, subscriptionId);
        verify(subscriptionService, never()).deleteClientSubscription(any());
    }

    @Test
    void get_event_types_controller_should_call_event_type_service() {
        var result = subscriptionController.getEventTypes();
        verify(eventTypeService).getAllEventTypes();
        assertThat(result.getStatusCode().value()).isEqualTo(200);
    }
}