package uk.gov.hmcts.cp.subscription.mappers;

import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.hmcts.cp.openapi.model.ClientSubscription;
import uk.gov.hmcts.cp.subscription.entities.ClientEntity;
import uk.gov.hmcts.cp.subscription.services.ClockService;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ClientMapper {

    @Mapping(source = "clientId", target = "id")
    @Mapping(source = "clientSubscription.clientSubscriptionId", target = "subscriptionId")
    @Mapping(source = "clientSubscription.notificationEndpoint.callbackUrl", target = "callbackUrl")
    @Mapping(target = "createdAt", expression = "java(clockService.nowOffsetUTC())")
    @Mapping(target = "updatedAt", expression = "java(clockService.nowOffsetUTC())")
    ClientEntity mapToClientEntity(@Context ClockService clockService, ClientSubscription clientSubscription, UUID clientId);
}
