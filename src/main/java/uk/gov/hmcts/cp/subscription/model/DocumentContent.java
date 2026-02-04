package uk.gov.hmcts.cp.subscription.model;

import lombok.Builder;
import lombok.Value;
import org.springframework.http.MediaType;

/**
 * Holds binary document content with metadata for download response.
 */
@Value
@Builder
public class DocumentContent {
    byte[] body;
    MediaType contentType;
    String fileName;
}
