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
import uk.gov.hmcts.cp.subscription.repositories.SubscriptionRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentMappingRepository documentMappingRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ClockService clockService;
    private final DocumentMapper documentMapper;
    private final MaterialApi materialApi;
    private final MaterialClient materialClient;

    @Transactional
    public UUID saveDocumentMapping(final UUID materialId, final EntityEventType eventType) {
        final DocumentMappingEntity entity = documentMapper.mapToEntity(clockService, materialId, eventType);
        return documentMappingRepository.save(entity).getDocumentId();
    }

    @Transactional(readOnly = true)
    public UUID getDocumentIdForMaterialId(final UUID materialId, final EntityEventType eventType) {
        return documentMappingRepository.findByMaterialIdAndEventType(materialId, eventType).get().getDocumentId();
    }

    public DocumentContent getDocumentContent(final UUID clientSubscriptionId, final UUID documentId) {
        final DocumentMappingEntity documentMapping = documentMappingRepository.findById(documentId).get();
        // lets move this validation into the controller with a specific method if required
        // or maybe we should add subscription-id into the document
        // And maybe make the repository query accept eventType not String so not converting
        if (!subscriptionRepository.existsByIdAndEventType(clientSubscriptionId, documentMapping.getEventType().name())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: subscription does not have access to this document");
        }
        final Material materialDetails = materialApi.getMaterialByMaterialId(documentMapping.getMaterialId(), null, null);
        ResponseEntity<byte[]> document = materialClient.getMaterialDocument(materialDetails.getContentUrl());
        return DocumentContent.builder()
                .body(document.getBody())
                .contentType(MediaType.APPLICATION_PDF)
                .fileName(materialDetails.getMetadata().getFileName())
                .build();
    }
}
