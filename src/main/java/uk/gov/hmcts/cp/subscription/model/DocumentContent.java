package uk.gov.hmcts.cp.subscription.model;

import lombok.Builder;
import lombok.Value;

/**
 * Holds binary document content with metadata for download response.
 */
@Value
@Builder
public class DocumentContent {
    byte[] body;
    String contentType;
    String fileName;
}
