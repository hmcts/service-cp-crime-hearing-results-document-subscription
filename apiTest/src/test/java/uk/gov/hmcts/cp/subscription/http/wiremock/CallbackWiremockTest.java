package uk.gov.hmcts.cp.subscription.http.wiremock;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class CallbackWiremockTest {
    private static final String CORRELATION_ID_KEY = "X-Correlation-Id";

    private RestClient restClient = RestClient.create();
    private RestClient tlsRestClient = trustLocalhostRestClient();
    private String wireMockBaseUrl = "http://localhost:8090";
    private String wireMockHttpsBaseUrl = "https://localhost:8443";
    private String callbackUrl = "/callback/notify";

    UUID correlationId = UUID.randomUUID();

    @Test
    void mock_callback_client_should_respond_ok() {
        ResponseEntity<String> response = restClient
                .post()
                .uri(wireMockBaseUrl + callbackUrl)
                .header(CORRELATION_ID_KEY, correlationId.toString())
                .retrieve()
                .toEntity(String.class);
        log.info("callback response:{}", response.getBody());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void mock_https_callback_client_should_respond_ok() {
        ResponseEntity<String> response = tlsRestClient
                .post()
                .uri(wireMockHttpsBaseUrl + callbackUrl)
                .header(CORRELATION_ID_KEY, correlationId.toString())
                .retrieve()
                .toEntity(String.class);
        log.info("callback response:{}", response.getBody());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private static RestClient trustLocalhostRestClient() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            }}, null);

            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
                @Override
                protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                    if (connection instanceof HttpsURLConnection httpsConnection) {
                        httpsConnection.setSSLSocketFactory(sslContext.getSocketFactory());
                        httpsConnection.setHostnameVerifier((hostname, session) -> "localhost".equals(hostname));
                    }
                    super.prepareConnection(connection, httpMethod);
                }
            };

            return RestClient.builder()
                    .requestFactory(factory)
                    .build();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException("Failed to create trust-all SSL context", e);
        }
    }
}
