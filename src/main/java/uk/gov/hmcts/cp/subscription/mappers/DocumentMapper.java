package uk.gov.hmcts.cp.subscription.mappers;

import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueMappingStrategy;
import uk.gov.hmcts.cp.subscription.entities.DocumentMappingEntity;
import uk.gov.hmcts.cp.subscription.model.EntityEventType;
import uk.gov.hmcts.cp.subscription.services.ClockService;

import java.util.UUID;

@Mapper(componentModel = "spring",
        nullValueIterableMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT,
        nullValueMapMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
public interface DocumentMapper {

    /**
     * Maps a materialId and eventType to a new DocumentMappingEntity.
     * Sets the createdAt timestamp using ClockService.
     */
    @Mapping(target = "documentId", expression = "java(null)")
    @Mapping(source = "materialId", target = "materialId")
    @Mapping(source = "eventType", target = "eventType")
    @Mapping(target = "createdAt", expression = "java(clockService.nowOffsetUTC())")
    DocumentMappingEntity mapToEntity(@Context ClockService clockService, UUID materialId, EntityEventType eventType);
}

