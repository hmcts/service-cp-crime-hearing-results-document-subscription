package uk.gov.hmcts.cp.subscription.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.hmcts.cp.subscription.entities.ClientEventEntity;
import uk.gov.hmcts.cp.subscription.entities.EventTypeEntity;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ClientEventMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(source = "subscriptionId", target = "subscriptionId")
    @Mapping(source = "eventTypeEntity.id", target = "eventTypeId")
    ClientEventEntity mapToClientEventEntity(UUID subscriptionId, EventTypeEntity eventTypeEntity);

    default List<ClientEventEntity> mapToClientEventEntityList(UUID subscriptionId, Set<EventTypeEntity> eventTypeEntities) {
        return eventTypeEntities.stream()
                .map(eventTypeEntity -> mapToClientEventEntity(subscriptionId, eventTypeEntity))
                .collect(Collectors.toList());
    }
}
