package uk.gov.hmcts.cp.subscription.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.material.openapi.api.MaterialApi;
import uk.gov.hmcts.cp.material.openapi.model.MaterialMetadata;
import uk.gov.hmcts.cp.subscription.config.AppProperties;

import java.time.Duration;
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
    // Using await() runs in separate thread thus loses the MDC correlationId
    public MaterialMetadata waitForMaterialMetadata(final UUID materialId) {
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

    public MaterialMetadata getMaterialMetadata(final UUID materialId) {
        log.info("COLING MDC corId:{}", MDC.get("X-Correlation-Id"));
        return materialApi.getMaterialMetadataByMaterialId(materialId);
    }
}
