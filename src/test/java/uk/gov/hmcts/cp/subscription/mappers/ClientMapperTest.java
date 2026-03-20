package uk.gov.hmcts.cp.subscription.mappers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cp.openapi.model.ClientSubscription;
import uk.gov.hmcts.cp.openapi.model.NotificationEndpoint;
import uk.gov.hmcts.cp.subscription.entities.ClientEntity;
import uk.gov.hmcts.cp.subscription.services.ClockService;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientMapperTest {

    @Mock
    ClockService clockService;

    ClientMapper clientMapper = new ClientMapperImpl();

    @Test
    void mapToClientEntity_should_map_client_subscription_to_entity() {
        // Given
        UUID subscriptionId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        String callbackUrl = "https://example.com/callback";

        OffsetDateTime now = OffsetDateTime.now();
        when(clockService.nowOffsetUTC()).thenReturn(now);

        ClientSubscription clientSubscription = ClientSubscription.builder()
                .clientSubscriptionId(subscriptionId)
                .notificationEndpoint(NotificationEndpoint.builder()
                        .callbackUrl(callbackUrl)
                        .build())
                .build();

        // When
        ClientEntity result = clientMapper.mapToClientEntity(clockService, clientSubscription, clientId);

        // Then
        assertThat(result.getId()).isEqualTo(clientId); // ID should be ignored (set by service)
        assertThat(result.getSubscriptionId()).isEqualTo(subscriptionId);
        assertThat(result.getCallbackUrl()).isEqualTo(callbackUrl);
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();
        assertThat(result.getCreatedAt()).isEqualTo(result.getUpdatedAt()); // Both set to now
    }
}
