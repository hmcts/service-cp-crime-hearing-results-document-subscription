package uk.gov.hmcts.cp.subscription.unit.mappers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cp.openapi.model.ClientSubscription;
import uk.gov.hmcts.cp.openapi.model.ClientSubscriptionRequest;
import uk.gov.hmcts.cp.openapi.model.NotificationEndpoint;
import uk.gov.hmcts.cp.subscription.entities.ClientSubscriptionEntity;
import uk.gov.hmcts.cp.subscription.mappers.SubscriptionMapper;
import uk.gov.hmcts.cp.subscription.mappers.SubscriptionMapperImpl;
import uk.gov.hmcts.cp.subscription.model.EntityEventType;
import uk.gov.hmcts.cp.subscription.services.ClockService;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.cp.openapi.model.EventType.CUSTODIAL_RESULT;
import static uk.gov.hmcts.cp.openapi.model.EventType.PRISON_COURT_REGISTER_GENERATED;

@ExtendWith(MockitoExtension.class)
class SubscriptionMapperTest {

    @Mock
    ClockService clockService;

    SubscriptionMapper mapper = new SubscriptionMapperImpl();

    OffsetDateTime mockCreated = OffsetDateTime.of(2025, 12, 1, 11, 30, 50, 582007048, ZoneOffset.UTC);
    OffsetDateTime mockUpdated = OffsetDateTime.of(2025, 12, 2, 12, 40, 55, 234567890, ZoneOffset.UTC);
    UUID clientSubscriptionId = UUID.fromString("d730c6e1-66ba-4ef0-a3dd-0b9928faa76d");
    NotificationEndpoint notificationEndpoint = NotificationEndpoint.builder()
            .callbackUrl("https://example.com")
            .build();
    ClientSubscriptionEntity existing = ClientSubscriptionEntity.builder()
            .id(clientSubscriptionId)
            .clientId("test-client-id")
            .notificationEndpoint(notificationEndpoint.getCallbackUrl().toString())
            .eventTypes(mutableLisOfEventTypes())
            .createdAt(mockCreated)
            .updatedAt(mockUpdated)
            .build();

    @Test
    void create_request_should_map_to_entity_with_sorted_types() {
        when(clockService.nowOffsetUTC()).thenReturn(mockCreated);
        ClientSubscriptionRequest request = ClientSubscriptionRequest.builder()
                .notificationEndpoint(notificationEndpoint)
                .eventTypes(List.of(PRISON_COURT_REGISTER_GENERATED, CUSTODIAL_RESULT))
                .build();

        ClientSubscriptionEntity entity = mapper.mapCreateRequestToEntity(clockService, request);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getNotificationEndpoint()).isEqualTo("https://example.com");
        assertThat(entity.getEventTypes().toString()).isEqualTo("[CUSTODIAL_RESULT, PRISON_COURT_REGISTER_GENERATED]");
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isNotNull();
    }

    @Test
    void update_request_should_map_to_entity_with_sorted_types() {
        NotificationEndpoint updatedEndpoint = NotificationEndpoint.builder()
                .callbackUrl("https://updated.com")
                .build();
        ClientSubscriptionRequest request = ClientSubscriptionRequest.builder()
                .notificationEndpoint(updatedEndpoint)
                .eventTypes(List.of(CUSTODIAL_RESULT))
                .build();
        when(clockService.nowOffsetUTC()).thenReturn(mockUpdated);
        ClientSubscriptionEntity entity = mapper.mapUpdateRequestToEntity(clockService, existing, request);

        assertThat(entity.getId()).isEqualTo(clientSubscriptionId);
        assertThat(entity.getClientId()).isEqualTo("test-client-id");
        assertThat(entity.getNotificationEndpoint()).isEqualTo("https://updated.com");
        assertThat(entity.getEventTypes().toString()).isEqualTo("[CUSTODIAL_RESULT]");
        assertThat(entity.getCreatedAt()).isEqualTo(mockCreated);
        assertThat(entity.getUpdatedAt()).isEqualTo(mockUpdated);
    }

    @Test
    void entity_should_map_to_response() {
        ClientSubscription subscription = mapper.mapEntityToResponse(clockService, existing);

        assertThat(subscription.getClientSubscriptionId()).isEqualTo(clientSubscriptionId);
        assertThat(subscription.getNotificationEndpoint()).isEqualTo(notificationEndpoint);
        assertThat(subscription.getEventTypes().toString()).isEqualTo("[CUSTODIAL_RESULT, PRISON_COURT_REGISTER_GENERATED]");
        assertThat(subscription.getCreatedAt()).isEqualTo(mockCreated.toInstant());
        assertThat(subscription.getUpdatedAt()).isEqualTo(mockUpdated.toInstant());
    }

    private List<EntityEventType> mutableLisOfEventTypes() {
        List<EntityEventType> mutableList = new ArrayList<>();
        mutableList.add(EntityEventType.CUSTODIAL_RESULT);
        mutableList.add(EntityEventType.PRISON_COURT_REGISTER_GENERATED);
        return mutableList;
    }
}
