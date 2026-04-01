package uk.gov.hmcts.cp.subscription.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.hmcts.cp.subscription.entities.ClientHmacEntity;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ClientHmacMapper {


    @Mapping(target = "id", ignore = true)
    ClientHmacEntity toEntity(final UUID subscriptionId, final String keyId);
}
