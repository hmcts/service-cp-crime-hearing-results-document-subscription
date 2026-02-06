package uk.gov.hmcts.cp.subscription.config;

import lombok.Builder;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Builder
@Getter
@Service
public class AppProperties {

    private int materialRetryIntervalMilliSecs;
    private int materialRetryTimeoutMilliSecs;

    public AppProperties(@Value("${material-client.retry.intervalMilliSecs}") final int materialRetryIntervalMilliSecs,
                         @Value("${material-client.retry.timeoutMilliSecs}") final int materialRetryTimeoutMilliSecs) {
        this.materialRetryIntervalMilliSecs = materialRetryIntervalMilliSecs;
        this.materialRetryTimeoutMilliSecs = materialRetryTimeoutMilliSecs;
    }
}
