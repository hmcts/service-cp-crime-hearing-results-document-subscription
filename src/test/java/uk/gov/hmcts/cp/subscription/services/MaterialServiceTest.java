package uk.gov.hmcts.cp.subscription.services;

import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import uk.gov.hmcts.cp.subscription.clients.MaterialClient;
import uk.gov.hmcts.cp.subscription.config.AppProperties;
import uk.gov.hmcts.cp.subscription.model.MaterialMetadata;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialServiceTest {
    @Mock
    AppProperties appProperties;
    @Mock
    MaterialClient materialClient;

    @InjectMocks
    MaterialService materialService;

    UUID materialId = randomUUID();
    MaterialMetadata materialMetadata = new MaterialMetadata();

    @Test
    void material_metadata_should_return(){
        when(materialClient.getMetadata(materialId)).thenReturn(materialMetadata);
        MaterialMetadata response = materialService.getMaterialMetadata(materialId);
        assertThat(response).isEqualTo(materialMetadata);
    }

    @Test
    void poll_should_return_true_when_metadata_available() {
        materialMetadata.setMaterialId(materialId);
        when(materialClient.getMetadata(materialId)).thenReturn(materialMetadata);
        final AtomicReference<MaterialMetadata> ref = new AtomicReference<>();

        boolean result = materialService.pollMaterialMetadata(materialId, Map.of(), ref);

        assertThat(result).isTrue();
        assertThat(ref.get().getMaterialId()).isEqualTo(materialId);
    }

    @Test
    void poll_should_return_false_when_exception_thrown() {
        when(materialClient.getMetadata(materialId))
                .thenThrow(new RuntimeException("Connection refused"));

        boolean result = materialService.pollMaterialMetadata(materialId, Map.of(), new AtomicReference<>());

        assertThat(result).isFalse();
    }

    @Test
    void poll_should_return_false_when_response_is_null() {
        when(materialClient.getMetadata(materialId)).thenReturn(null);

        boolean result = materialService.pollMaterialMetadata(materialId, Map.of(), new AtomicReference<>());

        assertThat(result).isFalse();
    }
}