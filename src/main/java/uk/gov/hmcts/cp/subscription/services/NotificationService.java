package uk.gov.hmcts.cp.subscription.services;

import static java.time.Duration.ofSeconds;
import static org.awaitility.Awaitility.await;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uk.gov.hmcts.cp.material.openapi.api.MaterialApi;
import uk.gov.hmcts.cp.material.openapi.model.MaterialMetadata;
import uk.gov.hmcts.cp.openapi.model.PcrEventPayload;
import uk.gov.hmcts.cp.subscription.model.EntityEventType;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
public class NotificationService {

    private final MaterialApi materialApi;
    private final DocumentService documentService;
    private final Duration waitTimeout;
    private final Duration pollInterval;

    @Autowired
    public NotificationService(final MaterialApi materialApi,
                               final DocumentService documentService) {
        this(materialApi, documentService, ofSeconds(30), ofSeconds(5));
    }

    public NotificationService(final MaterialApi materialApi,
                               final DocumentService documentService,
                               final Duration waitTimeout,
                               final Duration pollInterval) {
        this.materialApi = materialApi;
        this.documentService = documentService;
        this.waitTimeout = waitTimeout;
        this.pollInterval = pollInterval;
    }

    public void processInboundEvent(final PcrEventPayload pcrEventPayload) {
        final MaterialMetadata materialMetadata = waitForMaterialMetadata(pcrEventPayload.getMaterialId());

        final EntityEventType eventType = EntityEventType.valueOf(pcrEventPayload.getEventType().name());
        documentService.saveDocumentMapping(materialMetadata.getMaterialId(), eventType);
    }

    private MaterialMetadata waitForMaterialMetadata(final UUID materialId) {
        final AtomicReference<MaterialMetadata> materialResponse = new AtomicReference<>();
        await()
                .atMost(waitTimeout)
                .pollInterval(pollInterval)
                .until(() -> {
                    final MaterialMetadata response = materialApi.getMaterialMetadataByMaterialId(materialId);
                    materialResponse.set(response);
                    return response != null;
                });
        return materialResponse.get();
    }
}
