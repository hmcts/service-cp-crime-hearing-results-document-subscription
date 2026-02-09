package uk.gov.hmcts.cp.subscription.unit.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cp.openapi.model.ClientSubscription;
import uk.gov.hmcts.cp.openapi.model.ClientSubscriptionRequest;
import uk.gov.hmcts.cp.openapi.model.NotificationEndpoint;
import uk.gov.hmcts.cp.subscription.entities.ClientSubscriptionEntity;
import uk.gov.hmcts.cp.subscription.mappers.SubscriptionMapper;
import uk.gov.hmcts.cp.subscription.repositories.SubscriptionRepository;
import uk.gov.hmcts.cp.subscription.services.ClockService;
import uk.gov.hmcts.cp.subscription.services.SubscriptionService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.cp.openapi.model.EventType.PRISON_COURT_REGISTER_GENERATED;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    ClockService clockService;
    @Mock
    SubscriptionRepository subscriptionRepository;
    @Mock
    SubscriptionMapper mapper;
    @InjectMocks
    SubscriptionService subscriptionService;

    ClientSubscriptionRequest createRequest = ClientSubscriptionRequest.builder()
            .notificationEndpoint(NotificationEndpoint.builder().callbackUrl("https://example.com/callback").build())
            .eventTypes(List.of(PRISON_COURT_REGISTER_GENERATED))
            .build();
    ClientSubscriptionRequest updateRequest = ClientSubscriptionRequest.builder().build();
    ClientSubscriptionEntity requestEntity = ClientSubscriptionEntity.builder().build();
    ClientSubscriptionEntity savedEntity = ClientSubscriptionEntity.builder().build();
    ClientSubscriptionEntity updatedEntity = ClientSubscriptionEntity.builder().build();
    ClientSubscription response = ClientSubscription.builder().build();
    UUID subscriptionId = UUID.fromString("2ca16eb5-3998-4bb7-adce-4bb9b3b7223c");

    @Test
    void save_request_should_save_new_entity() {
        when(mapper.mapCreateRequestToEntity(clockService, createRequest)).thenReturn(requestEntity);
        when(subscriptionRepository.save(requestEntity)).thenReturn(savedEntity);
        when(mapper.mapEntityToResponse(clockService, savedEntity)).thenReturn(response);

        ClientSubscription result = subscriptionService.saveSubscription(createRequest);

        verify(mapper).mapCreateRequestToEntity(clockService, createRequest);
        verify(subscriptionRepository).save(requestEntity);
        assertThat(result).isEqualTo(response);
    }

    @Test
    void update_request_should_update_existing_entity() {
        when(subscriptionRepository.getReferenceById(subscriptionId)).thenReturn(savedEntity);
        when(mapper.mapUpdateRequestToEntity(clockService, savedEntity, updateRequest)).thenReturn(requestEntity);
        when(subscriptionRepository.save(requestEntity)).thenReturn(updatedEntity);
        when(mapper.mapEntityToResponse(clockService, updatedEntity)).thenReturn(response);

        ClientSubscription result = subscriptionService.updateSubscription(subscriptionId, updateRequest);

        verify(subscriptionRepository).save(requestEntity);
        assertThat(result).isEqualTo(response);
    }

    @Test
    void delete_should_delete_entity() {
        subscriptionService.deleteSubscription(subscriptionId);
        verify(subscriptionRepository).deleteById(subscriptionId);
    }
}