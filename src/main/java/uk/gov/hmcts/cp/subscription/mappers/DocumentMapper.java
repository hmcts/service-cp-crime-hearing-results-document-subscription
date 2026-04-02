package uk.gov.hmcts.cp.subscription.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.hmcts.cp.subscription.entities.DocumentMappingEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface DocumentMapper {
    @Mapping(source = "documentId", target = "documentId")
    @Mapping(source = "materialId", target = "materialId")
    @Mapping(source = "eventType", target = "eventType")
    @Mapping(source = "createdAt", target = "createdAt")
    DocumentMappingEntity mapToNewEntity(UUID documentId, UUID materialId, String eventType, OffsetDateTime createdAt);
}

