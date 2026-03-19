package uk.gov.hmcts.cp.subscription.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.cp.material.openapi.ApiClient;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MaterialApiConfigTest {

    private final MaterialApiConfig config = new MaterialApiConfig();

    @Test
    void materialApiClient_should_set_base_url() {
        ApiClient client = config.materialApiClient("http://material-service", "test-uid", mock(RestTemplate.class));

        assertThat(client.getBasePath()).isEqualTo("http://material-service");
    }

    @Test
    void materialApiClient_should_set_cjscppuid_default_header() throws Exception {
        ApiClient client = config.materialApiClient("http://material-service", "test-uid", mock(RestTemplate.class));

        Field field = ApiClient.class.getDeclaredField("defaultHeaders");
        field.setAccessible(true);
        HttpHeaders headers = (HttpHeaders) field.get(client);

        assertThat(headers.getFirst("CJSCPPUID")).isEqualTo("test-uid");
    }
}