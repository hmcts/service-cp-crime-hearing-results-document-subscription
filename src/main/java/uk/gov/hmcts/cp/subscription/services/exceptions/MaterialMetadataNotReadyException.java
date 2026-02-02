package uk.gov.hmcts.cp.subscription.services.exceptions;

/**
 * Thrown when material metadata is not ready yet (null response). Triggers retry via @Retryable.
 */
public class MaterialMetadataNotReadyException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public MaterialMetadataNotReadyException(final String message) {
        super(message);
    }
}
