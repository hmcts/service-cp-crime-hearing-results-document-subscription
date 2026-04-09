package uk.gov.hmcts.cp.subscription.unit.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cp.openapi.model.EventPayload;
import uk.gov.hmcts.cp.subscription.model.MaterialMetadata;
import uk.gov.hmcts.cp.subscription.services.DocumentService;
import uk.gov.hmcts.cp.subscription.services.MaterialService;
import uk.gov.hmcts.cp.subscription.services.NotificationService;

import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private DocumentService documentService;
    @Mock
    private MaterialService materialService;

    @InjectMocks
    private NotificationService notificationService;


    @Test
    void processInboundEvent_should_fetch_metadata_and_save_document() {
        UUID materialId = randomUUID();
        EventPayload payload = EventPayload.builder()
                .materialId(materialId)
                .eventType("PRISON_COURT_REGISTER_GENERATED")
                .build();
        MaterialMetadata materialMetadata = new MaterialMetadata();
        materialMetadata.setMaterialId(materialId);
        when(materialService.getMaterialMetadata(materialId)).thenReturn(materialMetadata);

        notificationService.processInboundEvent(payload);

        verify(materialService).getMaterialMetadata(materialId);
        verify(documentService).saveDocumentMapping(eq(materialId), eq("PRISON_COURT_REGISTER_GENERATED"));
    }
}
