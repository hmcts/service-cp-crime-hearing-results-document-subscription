package uk.gov.hmcts.cp.subscription.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.material.openapi.model.MaterialMetadata;
import uk.gov.hmcts.cp.openapi.model.EventPayload;
import uk.gov.hmcts.cp.subscription.model.EntityEventType;

import java.util.UUID;

@Service
@Slf4j
@AllArgsConstructor
public class NotificationService {

    private final MaterialService materialService;
    private final DocumentService documentService;

    public UUID processInboundEvent(final EventPayload eventPayload) {
        final MaterialMetadata materialMetadata = materialService.waitForMaterialMetadata(eventPayload.getMaterialId());
        final EntityEventType eventType = EntityEventType.valueOf(eventPayload.getEventType().name());
        return documentService.saveDocumentMapping(materialMetadata.getMaterialId(), eventType);
    }
}
