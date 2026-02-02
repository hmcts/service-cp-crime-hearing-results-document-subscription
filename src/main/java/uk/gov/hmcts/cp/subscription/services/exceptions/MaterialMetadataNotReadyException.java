package uk.gov.hmcts.cp.subscription.services.exceptions;

/**
 * Thrown when material metadata is not ready yet (null response). Triggers retry via @Retryable.
 */
public class MaterialMetadataNotReadyException extends RuntimeException {
    public MaterialMetadataNotReadyException(final String message) {
        super(message);
    }
}
