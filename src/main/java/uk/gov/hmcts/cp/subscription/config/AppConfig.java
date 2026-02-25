package uk.gov.hmcts.cp.subscription.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.cp.subscription.services.ClockService;
import uk.gov.hmcts.cp.subscription.services.JsonMapper;
import uk.gov.hmcts.cp.subscription.util.JwtTokenParser;

import java.time.Clock;

@Configuration
public class AppConfig {

    @Bean
    public JwtTokenParser jwtTokenParser(JsonMapper jsonMapper) {
        return new JwtTokenParser(jsonMapper);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public RestClient restClient() {
        return RestClient.builder().build();
    }

    @Bean
    public ClockService clockService() {
        return new ClockService(Clock.systemDefaultZone());
    }
}
