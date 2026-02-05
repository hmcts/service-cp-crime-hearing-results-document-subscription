package uk.gov.hmcts.cp.subscription.unit.services;

import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cp.material.openapi.api.MaterialApi;
import uk.gov.hmcts.cp.material.openapi.model.MaterialMetadata;
import uk.gov.hmcts.cp.openapi.model.EventType;
import uk.gov.hmcts.cp.openapi.model.PcrEventPayload;
import uk.gov.hmcts.cp.subscription.config.AppProperties;
import uk.gov.hmcts.cp.subscription.services.DocumentService;
import uk.gov.hmcts.cp.subscription.services.NotificationService;

import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.cp.subscription.model.EntityEventType.PRISON_COURT_REGISTER_GENERATED;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    AppProperties appProperties;
    @Mock
    private MaterialApi materialApi;
    @Mock
    private DocumentService documentService;

    @InjectMocks
    private NotificationService notificationService;


    @Test
    void shouldSaveDocumentMappingWithEventTypeWhenMetadataPresent() {
        when(appProperties.getMaterialRetryIntervalMilliSecs()).thenReturn(10);
        when(appProperties.getMaterialRetryTimeoutMilliSecs()).thenReturn(50);
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
        when(appProperties.getMaterialRetryIntervalMilliSecs()).thenReturn(10);
        when(appProperties.getMaterialRetryTimeoutMilliSecs()).thenReturn(45);
        UUID materialId = randomUUID();
        PcrEventPayload payload = PcrEventPayload.builder()
                .materialId(materialId)
                .eventType(EventType.PRISON_COURT_REGISTER_GENERATED)
                .build();
        when(materialApi.getMaterialMetadataByMaterialId(any(UUID.class))).thenReturn(null);

        assertThrows(ConditionTimeoutException.class, () -> notificationService.processInboundEvent(payload));

        verify(materialApi, times(3)).getMaterialMetadataByMaterialId(materialId);
        verify(documentService, never()).saveDocumentMapping(eq(materialId), eq(PRISON_COURT_REGISTER_GENERATED));
    }
}
