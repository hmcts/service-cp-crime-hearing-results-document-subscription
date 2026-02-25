package uk.gov.hmcts.cp.servicebus.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.subscription.config.RetryServiceConfig;
import uk.gov.hmcts.cp.subscription.services.ClockService;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@AllArgsConstructor
@Service
public class ServiceBusRetryService {

    private RetryServiceConfig retryServiceConfig;
    private ClockService clockService;

    public int getDelaySecs(final int failureCount) {
        final List<Integer> retryConfig = retryServiceConfig.getRetryDelaySeconds();
        final int retryIndex = failureCount < retryConfig.size() ? failureCount : retryConfig.size() - 1;
        final int retryDelaySecs = retryConfig.get(retryIndex);
        log.info("retry delay {} seconds", retryDelaySecs);
        return retryDelaySecs;
    }

    public OffsetDateTime getNextTryTime(final int failureCount) {
        return clockService.nowOffsetUTC().plusSeconds(getDelaySecs(failureCount));
    }
}
