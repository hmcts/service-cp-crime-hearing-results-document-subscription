package uk.gov.hmcts.cp.subscription.services.exceptions;

/**
 * Thrown when callbackUrl delivery fails (network error or non-2xx response). Triggers retry via @Retryable.
 */
public class CallbackUrlDeliveryException extends RuntimeException {
    public CallbackUrlDeliveryException(final String message) {
        super(message);
    }

    public CallbackUrlDeliveryException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
