package uk.gov.hmcts.cp.subscription.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.cp.material.openapi.ApiClient;
import uk.gov.hmcts.cp.material.openapi.api.MaterialApi;

@Configuration
public class MaterialApiConfig {

    @Bean
    public ApiClient materialApiClient(
            @Value("${material-client.url}") final String baseUrl) {
        final ApiClient client = new ApiClient();
        client.setBasePath(baseUrl);
        return client;
    }

    @Bean
    public MaterialApi materialApi(final ApiClient materialApiClient) {
        return new MaterialApi(materialApiClient);
    }
}
