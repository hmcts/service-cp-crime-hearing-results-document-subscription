package uk.gov.hmcts.cp.subscription.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.material.openapi.api.MaterialApi;
import uk.gov.hmcts.cp.material.openapi.model.MaterialMetadata;
import uk.gov.hmcts.cp.subscription.config.AppProperties;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.awaitility.Awaitility.await;

@Service
@Slf4j
@RequiredArgsConstructor
public class MaterialService {

    private final AppProperties appProperties;
    private final MaterialApi materialApi;

    // TODO remove once we switch to async only
    public MaterialMetadata waitForMaterialMetadata(final UUID materialId) {
        final Map<String, String> mdcContext = MDC.getCopyOfContextMap();
        final AtomicReference<MaterialMetadata> materialResponse = new AtomicReference<>();
        await()
                .pollInterval(Duration.ofMillis(appProperties.getMaterialRetryIntervalMilliSecs()))
                .atMost(Duration.ofMillis(appProperties.getMaterialRetryTimeoutMilliSecs()))
                .until(() -> pollMaterialMetadata(materialId, mdcContext, materialResponse));
        return materialResponse.get();
    }

    @SuppressWarnings("PMD.OnlyOneReturn")
    public boolean pollMaterialMetadata(final UUID materialId, final Map<String, String> mdcContext,
                                 final AtomicReference<MaterialMetadata> materialResponse) {
        if (mdcContext != null) {
            MDC.setContextMap(mdcContext);
        }
        try {
            final MaterialMetadata response = materialApi.getMaterialMetadataByMaterialId(materialId);
            materialResponse.set(response);
            return response != null;
        } catch (Exception e) {
            log.warn("Material metadata not available for materialId: {}, retrying...", materialId, e);
            return false;
        }
    }

    public MaterialMetadata getMaterialMetadata(final UUID materialId) {
        return materialApi.getMaterialMetadataByMaterialId(materialId);
    }
}
