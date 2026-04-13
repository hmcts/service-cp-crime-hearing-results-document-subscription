package uk.gov.hmcts.cp.subscription.config;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Builder
@Getter
@Service
@Slf4j
public class AppProperties {

    private final EnvironmentName environmentName;
    private final int materialRetryIntervalMilliSecs;
    private final int materialRetryTimeoutMilliSecs;
    private final int callbackRetryIntervalMilliSecs;
    private final int callbackRetryTimeoutMilliSecs;

    public AppProperties(
            @Value("${environment.name}") final EnvironmentName environmentName,
            @Value("${material-client.retry.intervalMilliSecs}") final int materialRetryIntervalMilliSecs,
            @Value("${material-client.retry.timeoutMilliSecs}") final int materialRetryTimeoutMilliSecs,
            @Value("${callback-client.retry.intervalMilliSecs}") final int callbackRetryIntervalMilliSecs,
            @Value("${callback-client.retry.timeoutMilliSecs}") final int callbackRetryTimeoutMilliSecs) {
        this.environmentName = environmentName;
        this.materialRetryIntervalMilliSecs = materialRetryIntervalMilliSecs;
        this.materialRetryTimeoutMilliSecs = materialRetryTimeoutMilliSecs;
        this.callbackRetryIntervalMilliSecs = callbackRetryIntervalMilliSecs;
        this.callbackRetryTimeoutMilliSecs = callbackRetryTimeoutMilliSecs;
        log.info("Initialised AppProperties with environmentName:{}", environmentName);
    }
}
