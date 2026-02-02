package uk.gov.hmcts.cp.subscription.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.cp.openapi.model.PcrEventPayload;

/**
 * Outbound payload sent to subscribers containing the original PCR payload and the document id.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PcrOutboundPayload {
    private PcrEventPayload pcrEventPayload;
    private String documentId;
}
