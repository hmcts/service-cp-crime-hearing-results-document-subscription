package uk.gov.hmcts.cp.subscription.services;

import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cp.material.openapi.api.MaterialApi;
import uk.gov.hmcts.cp.material.openapi.model.MaterialMetadata;
import uk.gov.hmcts.cp.subscription.config.AppProperties;

import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialServiceTest {
    @Mock
    AppProperties appProperties;
    @Mock
    MaterialApi materialApi;

    @InjectMocks
    MaterialService materialService;

    UUID materialId = randomUUID();
    MaterialMetadata materialMetadata = new MaterialMetadata();

    @Test
    void material_metadata_should_return(){
        when(materialApi.getMaterialMetadataByMaterialId(materialId)).thenReturn(materialMetadata);
        MaterialMetadata response = materialService.getMaterialMetadata(materialId);
        assertThat(response).isEqualTo(materialMetadata);
    }

    @Test
    void material_ready_should_be_returned() {
        when(appProperties.getMaterialRetryIntervalMilliSecs()).thenReturn(100);
        when(appProperties.getMaterialRetryTimeoutMilliSecs()).thenReturn(400);
        materialMetadata.setMaterialId(materialId);
        when(materialApi.getMaterialMetadataByMaterialId(any(UUID.class))).thenReturn(materialMetadata);

        MaterialMetadata response = materialService.waitForMaterialMetadata(materialId);

        verify(materialApi, times(1)).getMaterialMetadataByMaterialId(materialId);
        assertThat(response.getMaterialId()).isEqualTo(materialId);
    }

    @Test
    void material_not_ready_should_throw_timeout_exception() {
        when(appProperties.getMaterialRetryIntervalMilliSecs()).thenReturn(100);
        when(appProperties.getMaterialRetryTimeoutMilliSecs()).thenReturn(400);
        when(materialApi.getMaterialMetadataByMaterialId(materialId)).thenReturn(null);

        assertThrows(ConditionTimeoutException.class, () -> materialService.waitForMaterialMetadata(materialId));

        verify(materialApi, times(3)).getMaterialMetadataByMaterialId(materialId);
    }
}