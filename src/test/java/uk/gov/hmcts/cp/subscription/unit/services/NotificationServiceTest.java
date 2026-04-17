package uk.gov.hmcts.cp.subscription.unit.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.cp.openapi.model.EventPayload;
import uk.gov.hmcts.cp.subscription.model.MaterialMetadata;
import uk.gov.hmcts.cp.subscription.services.DocumentService;
import uk.gov.hmcts.cp.subscription.services.MaterialService;
import uk.gov.hmcts.cp.subscription.services.NotificationService;

import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    void notification_should_save_document() {
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

    @Test
    void notification_should_propagate_exception_when_event_type_is_unknown() {
        UUID materialId = randomUUID();
        EventPayload payload = EventPayload.builder()
                .materialId(materialId)
                .eventType("UNKNOWN_EVENT")
                .build();
        MaterialMetadata materialMetadata = new MaterialMetadata();
        materialMetadata.setMaterialId(materialId);
        when(materialService.getMaterialMetadata(materialId)).thenReturn(materialMetadata);
        when(documentService.saveDocumentMapping(materialId, "UNKNOWN_EVENT"))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown event type: UNKNOWN_EVENT"));

        assertThatThrownBy(() -> notificationService.processInboundEvent(payload))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> {
                    ResponseStatusException ex = (ResponseStatusException) e;
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getReason()).contains("UNKNOWN_EVENT");
                });
    }
}
