package uk.gov.hmcts.cp.subscription.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.cp.material.openapi.ApiClient;
import uk.gov.hmcts.cp.material.openapi.api.MaterialApi;

/**
 * Configures the OpenAPI-generated Material API client with base URL from material-client.url.
 */
@Configuration
public class MaterialApiConfig {

    @Bean
    public ApiClient materialApiClient(
            @Value("${material-client.url}") final String baseUrl,
            final RestTemplate restTemplate) {
        final ApiClient client = new ApiClient(restTemplate);
        client.setBasePath(baseUrl);
        return client;
    }

    @Bean
    public MaterialApi materialApi(final ApiClient materialApiClient) {
        return new MaterialApi(materialApiClient);
    }
}
