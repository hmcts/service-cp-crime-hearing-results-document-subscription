package uk.gov.hmcts.cp.subscription.config;

import lombok.Builder;
import lombok.Value;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.Map;

@Value
@Builder
public class RetryTemplateConfig {

    int maxAttempts;
    long initialDelayMs;
    double multiplier;
    long maxDelayMs;

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

    public static RetryTemplateConfig retryConfig() {
        return RetryTemplateConfig.builder()
                .maxAttempts(3)
                .initialDelayMs(50)
                .multiplier(2)
                .maxDelayMs(5000)
                .build();
    }
}
