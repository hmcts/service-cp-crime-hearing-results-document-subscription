package uk.gov.hmcts.cp.subscription.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;

@Configuration
public class SSLTrustingRestTemplateConfig {

    private static final X509TrustManager TRUST_ALL = new X509TrustManager() {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    };

    @PostConstruct
    public void trustAllSsl() throws Exception {
        var ssl = SSLContext.getInstance("TLS");
        ssl.init(null, new TrustManager[]{TRUST_ALL}, new SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(ssl.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
    }
}
