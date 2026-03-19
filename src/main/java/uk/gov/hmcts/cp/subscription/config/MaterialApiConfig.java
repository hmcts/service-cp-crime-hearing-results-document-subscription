package uk.gov.hmcts.cp.subscription.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.cp.material.openapi.ApiClient;
import uk.gov.hmcts.cp.material.openapi.api.MaterialApi;

@Configuration
public class MaterialApiConfig {

    public static final String CJSCPPUID_HEADER = "CJSCPPUID";

    @Bean
    public ApiClient materialApiClient(
            @Value("${material-client.url}") final String baseUrl,
            @Value("${material-client.cjscppuid}") final String cjscppuid,
            final RestTemplate restTemplate) {
        final ApiClient client = new ApiClient(restTemplate);
        client.setBasePath(baseUrl);
        client.addDefaultHeader(CJSCPPUID_HEADER, cjscppuid);
        return client;
    }

    @Bean
    public MaterialApi materialApi(final ApiClient materialApiClient) {
        return new MaterialApi(materialApiClient);
    }
}
