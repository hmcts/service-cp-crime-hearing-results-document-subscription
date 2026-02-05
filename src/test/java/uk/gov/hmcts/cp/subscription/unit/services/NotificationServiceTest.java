package uk.gov.hmcts.cp.subscription.unit.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.awaitility.core.ConditionTimeoutException;

import uk.gov.hmcts.cp.material.openapi.api.MaterialApi;
import uk.gov.hmcts.cp.material.openapi.model.MaterialMetadata;
import uk.gov.hmcts.cp.openapi.model.EventType;
import uk.gov.hmcts.cp.openapi.model.PcrEventPayload;
import uk.gov.hmcts.cp.subscription.services.DocumentService;
import uk.gov.hmcts.cp.subscription.services.NotificationService;

import java.util.UUID;

import static java.time.Duration.ofMillis;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.cp.subscription.model.EntityEventType.PRISON_COURT_REGISTER_GENERATED;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private MaterialApi materialApi;

    @Mock
    private DocumentService documentService;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                materialApi,
                documentService,
                ofMillis(100),
                ofMillis(10)
        );
    }

    @Test
    void shouldSaveDocumentMappingWithEventTypeWhenMetadataPresent() {
        UUID materialId = randomUUID();
        PcrEventPayload payload = PcrEventPayload.builder()
                .materialId(materialId)
                .eventType(EventType.PRISON_COURT_REGISTER_GENERATED)
                .build();
        MaterialMetadata materialMetadata = new MaterialMetadata();
        materialMetadata.setMaterialId(materialId);
        when(materialApi.getMaterialMetadataByMaterialId(materialId)).thenReturn(materialMetadata);

        notificationService.processInboundEvent(payload);

        verify(materialApi).getMaterialMetadataByMaterialId(materialId);
        verify(documentService).saveDocumentMapping(eq(materialId), eq(PRISON_COURT_REGISTER_GENERATED));
    }

    @Test
    void shouldThrowExceptionWhenMaterialMetadataNotReady() {
        UUID materialId = randomUUID();
        PcrEventPayload payload = PcrEventPayload.builder()
                .materialId(materialId)
                .eventType(EventType.PRISON_COURT_REGISTER_GENERATED)
                .build();
        when(materialApi.getMaterialMetadataByMaterialId(any(UUID.class))).thenReturn(null);

        assertThrows(ConditionTimeoutException.class, () -> notificationService.processInboundEvent(payload));
    }
}
