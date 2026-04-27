package uk.gov.hmcts.cp.subscription.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cp.subscription.clients.MaterialClient;
import uk.gov.hmcts.cp.subscription.model.MaterialMetadata;

import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialServiceTest {
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
}
