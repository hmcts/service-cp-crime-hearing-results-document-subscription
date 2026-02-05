package uk.gov.hmcts.cp.subscription.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.material.openapi.api.MaterialApi;
import uk.gov.hmcts.cp.material.openapi.model.MaterialMetadata;
import uk.gov.hmcts.cp.openapi.model.PcrEventPayload;
import uk.gov.hmcts.cp.subscription.config.AppProperties;
import uk.gov.hmcts.cp.subscription.model.EntityEventType;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.awaitility.Awaitility.await;

@Service
@Slf4j
@AllArgsConstructor
public class NotificationService {

    private final AppProperties appProperties;
    private final MaterialApi materialApi;
    private final DocumentService documentService;

    public void processInboundEvent(final PcrEventPayload pcrEventPayload) {
        final MaterialMetadata materialMetadata = waitForMaterialMetadata(pcrEventPayload.getMaterialId());
        final EntityEventType eventType = EntityEventType.valueOf(pcrEventPayload.getEventType().name());
        documentService.saveDocumentMapping(materialMetadata.getMaterialId(), eventType);
    }

    private MaterialMetadata waitForMaterialMetadata(final UUID materialId) {
        final AtomicReference<MaterialMetadata> materialResponse = new AtomicReference<>();
        await()
                .pollInterval(Duration.ofMillis(appProperties.getMaterialRetryIntervalMilliSecs()))
                .atMost(Duration.ofMillis(appProperties.getMaterialRetryTimeoutMilliSecs()))
                .until(() -> {
                    final MaterialMetadata response = materialApi.getMaterialMetadataByMaterialId(materialId);
                    materialResponse.set(response);
                    return response != null;
                });
        return materialResponse.get();
    }
}
