package uk.gov.hmcts.cp.subscription.unit.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cp.hmac.managers.HmacManager;
import uk.gov.hmcts.cp.hmac.model.KeyPair;
import uk.gov.hmcts.cp.openapi.model.ClientSubscription;
import uk.gov.hmcts.cp.openapi.model.ClientSubscriptionRequest;
import uk.gov.hmcts.cp.openapi.model.NotificationEndpoint;
import uk.gov.hmcts.cp.subscription.entities.ClientEntity;
import uk.gov.hmcts.cp.subscription.entities.ClientEventEntity;
import uk.gov.hmcts.cp.subscription.entities.ClientHmacEntity;
import uk.gov.hmcts.cp.subscription.mappers.ClientEntityMapper;
import uk.gov.hmcts.cp.subscription.mappers.ClientEventEntityMapper;
import uk.gov.hmcts.cp.subscription.mappers.ClientHmacMapper;
import uk.gov.hmcts.cp.subscription.mappers.ClientSubscriptionMapper;
import uk.gov.hmcts.cp.subscription.repositories.ClientEventRepository;
import uk.gov.hmcts.cp.subscription.repositories.ClientHmacRepository;
import uk.gov.hmcts.cp.subscription.repositories.ClientRepository;
import uk.gov.hmcts.cp.subscription.services.ClockService;
import uk.gov.hmcts.cp.subscription.services.SubscriptionService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    ClockService clockService;
    @Mock
    HmacManager hmacManager;
    @Mock
    ClientRepository clientRepository;
    @Mock
    ClientHmacMapper clientHmacMapper;
    @Mock
    ClientHmacRepository clientHmacRepository;
    @Mock
    ClientEventRepository clientEventRepository;
    @Mock
    ClientEntityMapper clientEntityMapper;
    @Mock
    ClientEventEntityMapper clientEventEntityMapper;
    @Mock
    ClientSubscriptionMapper clientSubscriptionMapper;

    @InjectMocks
    SubscriptionService subscriptionService;

    ClientSubscriptionRequest createRequest = ClientSubscriptionRequest.builder()
            .notificationEndpoint(NotificationEndpoint.builder().callbackUrl("https://example.com/callback").build())
            .eventTypes(List.of("PRISON_COURT_REGISTER_GENERATED"))
            .build();
    ClientSubscriptionRequest updateRequest = ClientSubscriptionRequest.builder()
            .eventTypes(List.of("PRISON_COURT_REGISTER_GENERATED"))
            .build();
    UUID subscriptionId = UUID.fromString("2ca16eb5-3998-4bb7-adce-4bb9b3b7223c");
    UUID clientId = UUID.fromString("11111111-2222-3333-4444-555555555555");
    ClientEntity clientEntity = ClientEntity.builder()
            .subscriptionId(subscriptionId)
            .clientId(clientId)
            .build();
    ClientHmacEntity clientHmacEntity = ClientHmacEntity.builder().build();
    ClientEntity updatedClientEntity = ClientEntity.builder()
            .subscriptionId(subscriptionId)
            .clientId(clientId)
            .callbackUrl("https://example.com/updated-callback")
            .build();
    ClientEventEntity clientEventEntity = ClientEventEntity.builder()
            .subscriptionId(subscriptionId)
            .id(1L)
            .build();
    ClientSubscription response = ClientSubscription.builder().build();
    KeyPair hmacKeyPair = KeyPair.builder().keyId("kid-1").secret("secret-1".getBytes()).build();

    @Test
    void create_request_should_save_new_entity() {
        when(clientEntityMapper.toEntity(clockService, createRequest, clientId)).thenReturn(clientEntity);
        when(clientHmacMapper.toEntity(subscriptionId, hmacKeyPair.getKeyId())).thenReturn(clientHmacEntity);
        when(clientEventEntityMapper.toEntity(subscriptionId, 1L)).thenReturn(clientEventEntity);
        when(hmacManager.createAndStoreNewKey()).thenReturn(hmacKeyPair);
        when(clientSubscriptionMapper.toDto(clientEntity, List.of("PRISON_COURT_REGISTER_GENERATED"), hmacKeyPair)).thenReturn(response);
        when(clientRepository.save(clientEntity)).thenReturn(clientEntity);

        ClientSubscription result = subscriptionService.createClientSubscription(createRequest, clientId, List.of(1L));

        assertThat(result).isEqualTo(response);
        verify(clientRepository).save(clientEntity);
        verify(clientHmacRepository).save(clientHmacEntity);
        verify(clientEventRepository).saveAll(List.of(clientEventEntity));
    }

    @Test
    void update_request_should_update_existing_entity() {
        when(clientEntityMapper.mapUpdateRequestToEntity(clientEntity, clockService, updateRequest)).thenReturn(updatedClientEntity);
        when(clientEventEntityMapper.toEntity(subscriptionId, 1L)).thenReturn(clientEventEntity);
        when(clientSubscriptionMapper.toDto(updatedClientEntity, List.of("PRISON_COURT_REGISTER_GENERATED"), null)).thenReturn(response);
        when(clientRepository.save(updatedClientEntity)).thenReturn(updatedClientEntity);

        ClientSubscription result = subscriptionService.updateClientSubscription(updateRequest, clientEntity, List.of(1L));

        assertThat(result).isEqualTo(response);
        verify(clientRepository).save(updatedClientEntity);
        verify(clientEventRepository).saveAll(List.of(clientEventEntity));
    }

    @Test
    void get_should_return_subscription_when_owned_by_client() {
        when(clientEventRepository.findEventNamesForSubscription(subscriptionId)).thenReturn(List.of("PRISON_COURT_REGISTER_GENERATED"));
        when(clientSubscriptionMapper.toDto(clientEntity, List.of("PRISON_COURT_REGISTER_GENERATED"), null)).thenReturn(response);

        ClientSubscription result = subscriptionService.getClientSubscription(clientEntity);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void delete_should_delete_entity_when_owned_by_client() {
        subscriptionService.deleteClientSubscription(clientEntity);

        InOrder inOrder = inOrder(clientEventRepository, clientRepository);
        inOrder.verify(clientEventRepository).deleteBySubscriptionId(subscriptionId);
        inOrder.verify(clientRepository).delete(clientEntity);
    }

    @Test
    void hasAccess_should_return_true_when_matching_event_exists() {
        UUID subscriptionId = UUID.randomUUID();

        when(clientEventRepository.countByClientSubscriptionAndEventName(subscriptionId, "PRISON_COURT_REGISTER_GENERATED")).thenReturn(1L);

        boolean result = subscriptionService.hasAccess(subscriptionId, "PRISON_COURT_REGISTER_GENERATED");
        assertThat(result).isTrue();
    }

    @Test
    void hasAccess_should_return_false_when_no_matching_event() {
        UUID subscriptionId = UUID.randomUUID();

        when(clientEventRepository.countByClientSubscriptionAndEventName(subscriptionId, "PRISON_COURT_REGISTER_GENERATED")).thenReturn(0L);

        boolean result = subscriptionService.hasAccess(subscriptionId, "PRISON_COURT_REGISTER_GENERATED");
        assertThat(result).isFalse();
    }
}