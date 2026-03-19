package uk.gov.hmcts.cp.subscription.config;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.cp.material.openapi.ApiClient;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.cp.subscription.config.MaterialApiConfig.CJSCPPUID_HEADER;

class MaterialApiConfigTest {

    @Mock
    private RestTemplate mockRestTemplate;

    private final MaterialApiConfig config = new MaterialApiConfig();

    String cjscppuid = "11111111-2222-3333-4444-666666666666";

    @Test
    void materialApiClient_should_set_base_url() {
        ApiClient client = config.materialApiClient("http://material-service", cjscppuid, mockRestTemplate);

        assertThat(client.getBasePath()).isEqualTo("http://material-service");
    }

    @Test
    void materialApiClient_should_set_cjscppuid_default_header() throws Exception {
        ApiClient client = config.materialApiClient("http://material-service", cjscppuid, mockRestTemplate);

        HttpHeaders headers = getDefaultHeaders(client);

        assertThat(headers.getFirst(CJSCPPUID_HEADER)).isEqualTo(cjscppuid);
    }

    private HttpHeaders getDefaultHeaders(ApiClient client) throws Exception {
        Field field = ApiClient.class.getDeclaredField("defaultHeaders");
        field.setAccessible(true);
        return (HttpHeaders) field.get(client);
    }
}