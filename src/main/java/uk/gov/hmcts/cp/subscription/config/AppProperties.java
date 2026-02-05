package uk.gov.hmcts.cp.subscription.config;

import lombok.Builder;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Builder
@Getter
@Service
public class AppProperties {

    int materialRetryIntervalMilliSecs;
    int materialRetryTimeoutMilliSecs;

    public AppProperties(@Value("${material-client.retry.intervalMilliSecs}") int materialRetryIntervalMilliSecs,
                         @Value("${material-client.retry.timeoutMilliSecs}") int materialRetryTimeoutMilliSecs) {
        this.materialRetryIntervalMilliSecs = materialRetryIntervalMilliSecs;
        this.materialRetryTimeoutMilliSecs = materialRetryTimeoutMilliSecs;
    }
}
