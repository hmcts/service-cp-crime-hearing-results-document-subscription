package uk.gov.hmcts.cp.subscription.integration.repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.cp.subscription.entities.DocumentMappingEntity;
import uk.gov.hmcts.cp.subscription.integration.IntegrationTestBase;
import uk.gov.hmcts.cp.subscription.model.EntityEventType;

import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;

class DocumentRepositoryTest extends IntegrationTestBase {

    @BeforeEach
    void beforeEach() {
        clearDocumentMappingTable();
    }

    @Transactional
    @Test
    void document_should_save_and_read_by_id() {
        UUID materialId = randomUUID();
        DocumentMappingEntity saved = insertDocument(materialId);

        DocumentMappingEntity found = documentMappingRepository.getReferenceById(saved.getDocumentId());

        assertThat(found.getDocumentId()).isEqualTo(saved.getDocumentId());
        assertThat(found.getMaterialId()).isEqualTo(materialId);
        assertThat(found.getEventType()).isEqualTo(EntityEventType.PRISON_COURT_REGISTER_GENERATED);
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Transactional
    @Test
    void document_should_save_with_custom_event_type() {
        UUID materialId = randomUUID();
        EntityEventType eventType = EntityEventType.CUSTODIAL_RESULT;
        DocumentMappingEntity saved = insertDocument(materialId, eventType);

        DocumentMappingEntity found = documentMappingRepository.getReferenceById(saved.getDocumentId());

        assertThat(found.getDocumentId()).isEqualTo(saved.getDocumentId());
        assertThat(found.getMaterialId()).isEqualTo(materialId);
        assertThat(found.getEventType()).isEqualTo(eventType);
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Transactional
    @Test
    void findByMaterialId_should_return_document_when_exists() {
        UUID materialId = randomUUID();
        DocumentMappingEntity saved = insertDocument(materialId);

        Optional<DocumentMappingEntity> found = documentMappingRepository.findByMaterialId(materialId);

        assertThat(found).isPresent();
        assertThat(found.get().getDocumentId()).isEqualTo(saved.getDocumentId());
        assertThat(found.get().getMaterialId()).isEqualTo(materialId);
    }

    @Transactional
    @Test
    void findByMaterialId_should_return_empty_when_not_exists() {
        UUID nonExistentMaterialId = randomUUID();

        Optional<DocumentMappingEntity> found = documentMappingRepository.findByMaterialId(nonExistentMaterialId);

        assertThat(found).isEmpty();
    }

    @Transactional
    @Test
    void document_should_delete_ok() {
        UUID materialId = randomUUID();
        DocumentMappingEntity saved = insertDocument(materialId);

        documentMappingRepository.deleteById(saved.getDocumentId());

        assertThat(documentMappingRepository.findAll()).isEmpty();
    }

    @Transactional
    @Test
    void should_save_multiple_documents_with_different_material_ids() {
        UUID materialId1 = randomUUID();
        UUID materialId2 = randomUUID();

        DocumentMappingEntity doc1 = insertDocument(materialId1);
        DocumentMappingEntity doc2 = insertDocument(materialId2);

        assertThat(documentMappingRepository.findAll()).hasSize(2);
        assertThat(documentMappingRepository.findByMaterialId(materialId1).get().getDocumentId())
                .isEqualTo(doc1.getDocumentId());
        assertThat(documentMappingRepository.findByMaterialId(materialId2).get().getDocumentId())
                .isEqualTo(doc2.getDocumentId());
    }
}
