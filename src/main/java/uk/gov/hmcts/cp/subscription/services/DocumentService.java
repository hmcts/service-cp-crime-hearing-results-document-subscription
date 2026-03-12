package uk.gov.hmcts.cp.subscription.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.cp.material.openapi.api.MaterialApi;
import uk.gov.hmcts.cp.material.openapi.model.Material;
import uk.gov.hmcts.cp.subscription.clients.MaterialClient;
import uk.gov.hmcts.cp.subscription.entities.DocumentMappingEntity;
import uk.gov.hmcts.cp.subscription.mappers.DocumentMapper;
import uk.gov.hmcts.cp.subscription.model.DocumentContent;
import uk.gov.hmcts.cp.subscription.model.EntityEventType;
import uk.gov.hmcts.cp.subscription.repositories.DocumentMappingRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentMappingRepository documentMappingRepository;
    private final ClockService clockService;
    private final DocumentMapper documentMapper;
    private final MaterialApi materialApi;
    private final MaterialClient materialClient;

    @Transactional
    public UUID saveDocumentMapping(final UUID materialId, final EntityEventType eventType) {
        final DocumentMappingEntity entity = documentMapper.mapToEntity(clockService, materialId, eventType);
        DocumentMappingEntity saved = documentMappingRepository.save(entity);
        log.info("saveDocumentMapping materialId:{} to documentId:{}", materialId, saved.getDocumentId());
        return saved.getDocumentId();
    }

    @Transactional
    public UUID getDocumentIdForMaterialId(final UUID materialId) {
        return documentMappingRepository.findByMaterialId(materialId).get().getDocumentId();
    }

    @Transactional
    public EntityEventType getEventTypeForDocument(final UUID documentId) {
        return documentMappingRepository.findById(documentId).get().getEventType();
    }

    public DocumentContent getDocumentContent(final UUID documentId) {
        final DocumentMappingEntity documentMapping = documentMappingRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found: " + documentId));
        final Material materialDetails = materialApi.getMaterialByMaterialId(documentMapping.getMaterialId(), null, null);
        final ResponseEntity<byte[]> document = materialClient.getMaterialDocument(materialDetails.getContentUrl());
        return DocumentContent.builder()
                .body(document.getBody())
                .contentType(MediaType.APPLICATION_PDF)
                .fileName(materialDetails.getMetadata().getFileName())
                .build();
    }
}
