package uk.gov.hmcts.cp.subscription.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.cp.openapi.model.PcrEventPayload;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PcrOutboundPayload {
    private PcrEventPayload pcrEventPayload;
    private String documentId;
}
