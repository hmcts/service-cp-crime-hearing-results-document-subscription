package uk.gov.hmcts.cp.subscription.mappers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cp.subscription.entities.ClientHmacEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ClientHmacMapperTest {

    @InjectMocks
    ClientHmacMapperImpl mapper;

    UUID subscriptionId = UUID.fromString("02ea90ff-e091-42f0-a1ca-fcac67c63a5a");
    String keyId = "kid-v1-encodedkey";

    @Test
    void map_to_entity_should_set_fields() {
        ClientHmacEntity entity = mapper.toEntity(subscriptionId, keyId);
        assertThat(entity.getId()).isNull();
        assertThat(entity.getSubscriptionId()).isEqualTo(subscriptionId);
        assertThat(entity.getKeyId()).isEqualTo(keyId);
    }
}