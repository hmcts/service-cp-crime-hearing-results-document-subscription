package uk.gov.hmcts.cp.subscription.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValueMappingStrategy;
import uk.gov.hmcts.cp.hmac.model.KeyPair;
import uk.gov.hmcts.cp.openapi.model.ClientSubscription;
import uk.gov.hmcts.cp.openapi.model.ClientSubscriptionRequest;
import uk.gov.hmcts.cp.openapi.model.EventType;
import uk.gov.hmcts.cp.openapi.model.NotificationEndpoint;
import uk.gov.hmcts.cp.subscription.entities.ClientSubscriptionEntity;
import uk.gov.hmcts.cp.subscription.model.EntityEventType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

@Mapper(componentModel = "spring",
        nullValueIterableMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT,
        nullValueMapMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
public interface SubscriptionMapper {

    @Mapping(target = "id", expression = "java(java.util.UUID.randomUUID())")
    @Mapping(source = "request.eventTypes", target = "eventTypes", qualifiedByName = "mapWithSortedEventTypes")
    @Mapping(source = "request.notificationEndpoint", target = "notificationEndpoint", qualifiedByName = "mapFromNotificationEndpoint")
    @Mapping(source = "now", target = "createdAt")
    @Mapping(source = "now", target = "updatedAt")
    ClientSubscriptionEntity mapCreateRequestToEntity(UUID clientId, ClientSubscriptionRequest request, OffsetDateTime now);

    @Mapping(source = "existing.id", target = "id")
    @Mapping(source = "existing.clientId", target = "clientId")
    @Mapping(source = "request.eventTypes", target = "eventTypes", qualifiedByName = "mapWithSortedEventTypes")
    @Mapping(source = "request.notificationEndpoint", target = "notificationEndpoint", qualifiedByName = "mapFromNotificationEndpoint")
    @Mapping(source = "existing.createdAt", target = "createdAt")
    @Mapping(source = "now", target = "updatedAt")
    ClientSubscriptionEntity mapUpdateRequestToEntity(ClientSubscriptionEntity existing, ClientSubscriptionRequest request, OffsetDateTime now);

    @Mapping(source = "entity.id", target = "clientSubscriptionId")
    @Mapping(target = "createdAt", expression = "java(entity.getCreatedAt().toInstant())")
    @Mapping(target = "updatedAt", expression = "java(entity.getUpdatedAt().toInstant())")
    @Mapping(source = "hmac", target = "hmac")
        //         response.setHmac(HmacCredentials.builder().keyId(keyPair.keyId()).secret(keyPair.secret()).build());
    ClientSubscription mapEntityToResponse(ClientSubscriptionEntity entity, KeyPair hmac);

    @Named("mapWithSortedEventTypes")
    static List<EntityEventType> sortedEventTypes(final List<EventType> events) {
        final List<String> sorted = events.stream().map(Enum::name).sorted().collect(toList());
        return sorted.stream().map(EntityEventType::valueOf).toList();
    }

    @Named("mapFromNotificationEndpoint")
    static String mapFromNotificationEndpoint(final NotificationEndpoint notificationEndpoint) {
        return notificationEndpoint.getCallbackUrl();
    }

    static NotificationEndpoint mapToNotificationEndpoint(final String endpointUrl) {
        return NotificationEndpoint.builder().callbackUrl(endpointUrl).build();
    }
}
