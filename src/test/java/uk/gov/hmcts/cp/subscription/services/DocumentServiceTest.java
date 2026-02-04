package uk.gov.hmcts.cp.subscription.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.cp.material.openapi.api.MaterialApi;
import uk.gov.hmcts.cp.material.openapi.model.Material;
import uk.gov.hmcts.cp.material.openapi.model.MaterialMetadata;
import uk.gov.hmcts.cp.subscription.clients.MaterialClient;
import uk.gov.hmcts.cp.subscription.entities.DocumentMappingEntity;
import uk.gov.hmcts.cp.subscription.mappers.DocumentMapper;
import uk.gov.hmcts.cp.subscription.model.DocumentContent;
import uk.gov.hmcts.cp.subscription.repositories.DocumentMappingRepository;
import uk.gov.hmcts.cp.subscription.repositories.SubscriptionRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.cp.subscription.model.EntityEventType.PRISON_COURT_REGISTER_GENERATED;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {
    @Mock
    ClockService clockService;
    @Mock
    DocumentMapper documentMapper;
    @Mock
    DocumentMappingRepository documentMappingRepository;
    @Mock
    MaterialApi materialApi;
    @Mock
    MaterialClient materialClient;
    @Mock
    SubscriptionRepository subscriptionRepository;

    @InjectMocks
    DocumentService documentService;

    UUID materialId = UUID.fromString("43bb8246-2bdf-487c-a0d3-160bbfd37777");
    UUID documentId = UUID.fromString("2e48f8f1-c057-48f7-92e5-c4183480ea3e");
    DocumentMappingEntity documentMappingEntity = DocumentMappingEntity.builder()
            .documentId(documentId)
            .materialId(materialId)
            .eventType(PRISON_COURT_REGISTER_GENERATED).build();
    UUID subscriptionId = UUID.fromString("906bdc7b-ea20-49d2-b9fb-a0ce83cc6371");


    @Test
    void save_document_should_save_entity() {
        when(documentMapper.mapToEntity(clockService, materialId, PRISON_COURT_REGISTER_GENERATED)).thenReturn(documentMappingEntity);
        when(documentMappingRepository.save(documentMappingEntity)).thenReturn(documentMappingEntity);
        documentService.saveDocumentMapping(materialId, PRISON_COURT_REGISTER_GENERATED);
        verify(documentMappingRepository).save(documentMappingEntity);
    }

    @Test
    void get_document_id_should_return_id() {
        when(documentMappingRepository.findByMaterialIdAndEventType(materialId, PRISON_COURT_REGISTER_GENERATED)).thenReturn(Optional.of(documentMappingEntity));
        UUID response = documentService.getDocumentIdForMaterialId(materialId, PRISON_COURT_REGISTER_GENERATED);
        assertThat(response).isEqualTo(documentId);
    }

    @Test
    void get_document_content_should_return_response() {
        when(documentMappingRepository.findById(documentId)).thenReturn(Optional.of(documentMappingEntity));
        when(subscriptionRepository.existsByIdAndEventType(subscriptionId, PRISON_COURT_REGISTER_GENERATED.name())).thenReturn(true);
        when(materialApi.getMaterialByMaterialId(materialId, null, null)).thenReturn(createMaterial());
        ResponseEntity<byte[]> document = ResponseEntity.ok("pdfcontent".getBytes());
        when(materialClient.getMaterialDocument("http://material-servce")).thenReturn(document);

        DocumentContent documentContent = documentService.getDocumentContent(subscriptionId, documentId);

        assertThat(documentContent.getBody()).isEqualTo("pdfcontent".getBytes());
        assertThat(documentContent.getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(documentContent.getFileName()).isEqualTo("file.pdf");
    }

    private Material createMaterial() {
        // TODO Lets make material have a builder so we can make it immutable!
        Material material = new Material();
        material.setContentUrl("http://material-servce");
        MaterialMetadata materialMetadata = new MaterialMetadata();
        materialMetadata.setFileName("file.pdf");
        material.setMetadata(materialMetadata);
        return material;
    }
}