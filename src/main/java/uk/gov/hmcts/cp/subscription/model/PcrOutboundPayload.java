package uk.gov.hmcts.cp.subscription.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.cp.openapi.model.EventType;
import uk.gov.hmcts.cp.openapi.model.PcrEventPayloadDefendant;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PcrOutboundPayload {

    private UUID eventId;
    private EventType eventType;
    private UUID documentId;
    private Instant timestamp;
    private PcrEventPayloadDefendant defendant;
}
