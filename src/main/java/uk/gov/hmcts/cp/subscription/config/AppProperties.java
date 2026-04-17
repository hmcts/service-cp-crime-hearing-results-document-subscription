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

    public AppProperties(
            @Value("${environment.name}") final EnvironmentName environmentName) {
        this.environmentName = environmentName;
        log.info("Initialised AppProperties with environmentName:{}", environmentName);
    }
}
