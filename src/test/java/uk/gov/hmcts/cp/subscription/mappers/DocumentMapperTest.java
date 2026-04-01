package uk.gov.hmcts.cp.subscription.mappers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cp.subscription.entities.DocumentMappingEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class DocumentMapperTest {

    @InjectMocks
    DocumentMapper documentMapper;

    @Test
    void mapper_should_create_entity() {
        UUID documentId = UUID.fromString("6ba2498a-c869-45fd-9259-15f74d134653");
        UUID materialId = UUID.fromString("245c0d96-ecfe-45e6-a2a2-d51cac9aa643");
        OffsetDateTime now = OffsetDateTime.now();
        DocumentMappingEntity result = documentMapper.mapToNewEntity(documentId, materialId, "PRISON_COURT_REGISTER_GENERATED", now);
        assertThat(result.getDocumentId()).isNotNull();
        assertThat(result.getMaterialId()).isEqualTo(materialId);
        assertThat(result.getEventType()).isEqualTo("PRISON_COURT_REGISTER_GENERATED");
        assertThat(result.getCreatedAt()).isEqualTo(now);
    }
}