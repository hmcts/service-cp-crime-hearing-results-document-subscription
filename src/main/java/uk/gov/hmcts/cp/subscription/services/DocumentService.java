package uk.gov.hmcts.cp.subscription.services;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import uk.gov.hmcts.cp.material.openapi.api.MaterialApi;
import uk.gov.hmcts.cp.material.openapi.model.Material;
import uk.gov.hmcts.cp.subscription.entities.DocumentMappingEntity;
import uk.gov.hmcts.cp.subscription.mappers.DocumentMapper;
import uk.gov.hmcts.cp.subscription.model.DocumentContent;
import uk.gov.hmcts.cp.subscription.repositories.DocumentMappingRepository;
import uk.gov.hmcts.cp.subscription.repositories.SubscriptionRepository;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import uk.gov.hmcts.cp.subscription.model.EntityEventType;

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
    private final RestTemplate restTemplate;

    /**
     * Saves a document mapping with the associated eventType.
     */
    @Transactional
    public DocumentMappingEntity saveDocumentMapping(final UUID materialId, final EntityEventType eventType) {
        final DocumentMappingEntity entity = documentMapper.mapToEntity(clockService, materialId, eventType);
        return documentMappingRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public UUID getDocumentIdForMaterialId(final UUID materialId, final EntityEventType eventType) {
        return documentMappingRepository.findByMaterialIdAndEventType(materialId, eventType).get().getDocumentId();
    }

    /**
     * Returns document content as binary by fetching from the Material contentUrl (e.g. Azure Blob).
     * Validates subscription access, then fetches the file from the URL and returns bytes with metadata.
     */
    public DocumentContent getDocumentContentAsBinary(final UUID clientSubscriptionId, final UUID documentId) {
        final DocumentMappingEntity document = documentMappingRepository.findById(documentId).get();
        if (!subscriptionRepository.existsByIdAndEventType(clientSubscriptionId, document.getEventType().name())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: subscription does not have access to this document");
        }
        final Material material = materialApi.getMaterialByMaterialId(document.getMaterialId().toString(), null, null);
        final ResponseEntity<byte[]> response = restTemplate.getForEntity(material.getContentUrl(), byte[].class);
        return DocumentContent.builder()
                .body(response.getBody())
                .contentType(response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE))
                .fileName(material.getMetadata().getFileName())
                .build();
    }
}
