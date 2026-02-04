package uk.gov.hmcts.cp.subscription.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.material.openapi.api.MaterialApi;
import uk.gov.hmcts.cp.material.openapi.model.MaterialMetadata;
import uk.gov.hmcts.cp.openapi.model.PcrEventPayload;
import uk.gov.hmcts.cp.subscription.model.EntityEventType;
import uk.gov.hmcts.cp.subscription.services.exceptions.MaterialMetadataNotReadyException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final MaterialApi materialApi;
    private final DocumentService documentService;
    @Qualifier("retryTemplate")
    private final RetryTemplate materialRetryTemplate;

    public void processInboundEvent(final PcrEventPayload pcrEventPayload) {
        final MaterialMetadata materialMetadata = materialRetryTemplate.execute(context ->
                waitForMaterialMetadata(pcrEventPayload.getMaterialId()));

        final EntityEventType eventType = EntityEventType.valueOf(pcrEventPayload.getEventType().name());
        documentService.saveDocumentMapping(materialMetadata.getMaterialId(), eventType);
    }

    private MaterialMetadata waitForMaterialMetadata(final UUID materialId) {
        final MaterialMetadata response = materialApi.getMaterialMetadataByMaterialId(materialId);
        if (response == null) {
            throw new MaterialMetadataNotReadyException("PCR - Material metadata not ready for materialId: " + materialId);
        }
        return response;
    }
}
