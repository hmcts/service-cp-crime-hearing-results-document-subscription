package uk.gov.hmcts.cp.subscription.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Getter
public class SubscriptionClientConfig {

    private final boolean oauthEnabled;
    private final String clientIdHeaderName;

    public SubscriptionClientConfig(
            @Value("${subscription.oauth-enabled:true}") final boolean oauthEnabled,
            @Value("${subscription.client-id-header:X-Client-Id}") final String clientIdHeaderName) {
        this.oauthEnabled = oauthEnabled;
        this.clientIdHeaderName = clientIdHeaderName;
        log.info("SubscriptionClientConfig oauthEnabled={} clientIdHeader={}", oauthEnabled, clientIdHeaderName);
    }
}
