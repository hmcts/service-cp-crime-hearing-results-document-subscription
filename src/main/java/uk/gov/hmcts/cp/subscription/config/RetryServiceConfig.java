package uk.gov.hmcts.cp.subscription.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@Getter
public class RetryServiceConfig {
    private final List<Integer> retryDelaySeconds;

    public RetryServiceConfig(
            @Value("${service-bus.retry-seconds}") final List<Integer> retryDelaySeconds
    ) {
        log.info("RetryConfigService using retryDelaySeconds {}", retryDelaySeconds);
        this.retryDelaySeconds = retryDelaySeconds;
    }
}
