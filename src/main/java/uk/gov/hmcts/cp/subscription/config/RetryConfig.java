package uk.gov.hmcts.cp.subscription.config;

import lombok.Builder;
import lombok.Value;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.Map;

/**
 * Common retry configuration for Material API and Callback URL delivery.
 * Supports exponential backoff and configurable retry policies.
 */
@Value
@Builder
public class RetryConfig {

    int maxAttempts;
    long initialDelayMs;
    double multiplier;
    long maxDelayMs;

    /**
     * Creates a RetryTemplate with exponential backoff that retries on the given exception types.
     */
    public RetryTemplate toRetryTemplate(final Map<Class<? extends Throwable>, Boolean> retryableExceptions) {
        final SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(maxAttempts, retryableExceptions);
        final ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(initialDelayMs);
        backOffPolicy.setMultiplier(multiplier);
        backOffPolicy.setMaxInterval(maxDelayMs);

        final RetryTemplate template = new RetryTemplate();
        template.setRetryPolicy(retryPolicy);
        template.setBackOffPolicy(backOffPolicy);
        return template;
    }

    public static RetryConfig retryConfig() {
        return RetryConfig.builder()
                .maxAttempts(3)
                .initialDelayMs(50)
                .multiplier(2)
                .maxDelayMs(5000)
                .build();
    }
}
