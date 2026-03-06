package uk.gov.hmcts.cp.subscription.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.material.openapi.model.MaterialMetadata;
import uk.gov.hmcts.cp.openapi.model.PcrEventPayload;
import uk.gov.hmcts.cp.subscription.model.EntityEventType;

@Service
@Slf4j
@AllArgsConstructor
public class NotificationService {

    private final MaterialService materialService;
    private final DocumentService documentService;

    public void processInboundEvent(final PcrEventPayload pcrEventPayload) {
        final MaterialMetadata materialMetadata = materialService.waitForMaterialMetadata(pcrEventPayload.getMaterialId());
        final EntityEventType eventType = EntityEventType.valueOf(pcrEventPayload.getEventType().name());
        documentService.saveDocumentMapping(materialMetadata.getMaterialId(), eventType);
    }
}
