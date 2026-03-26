package uk.gov.hmcts.cp.vault.services;

import com.azure.core.http.HttpHeaderName;
import com.azure.core.http.HttpPipeline;
import com.azure.core.http.HttpPipelineBuilder;
import com.azure.core.http.HttpPipelineCallContext;
import com.azure.core.http.HttpPipelineNextPolicy;
import com.azure.core.http.HttpPipelineNextSyncPolicy;
import com.azure.core.http.HttpResponse;
import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.core.http.policy.RetryPolicy;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

/**
 * Builds a SecretClient that works against the local Azure Key Vault emulator.
 * Three workarounds are bundled here so that production config stays clean
 *   Trust-all SSL — emulator uses a self-signed certificate
 *   Real JWT from GET /token— emulator validates actual tokens
 *   Custom HttpPipeline — bypasses  KeyVaultCredentialPolicy which throws ArrayIndexOutOfBoundsException when parsing the emulator's barer token
 */
@Slf4j
@Service
public class EmulatorSecretService {

    private static final String AUTH_HEADER_PREFIX = "Bearer ";
    private static final String TOKEN_PATH = "/token";

    public SecretClient build(final String vaultUrl) throws Exception {
        final var nettySslContext = SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();

        final HttpClient reactorClient = HttpClient.create()
                .secure(spec -> spec.sslContext(nettySslContext));

        final com.azure.core.http.HttpClient azureHttpClient =
                new NettyAsyncHttpClientBuilder(reactorClient).build();

        final String tokenUrl = toTokenUrl(vaultUrl);
        log.info("Fetching emulator token from {}", tokenUrl);
        final String jwtToken = fetchToken(reactorClient, tokenUrl);
        log.info("Emulator token obtained (length={})", jwtToken.length());

        final HttpPipeline pipeline = buildPipeline(azureHttpClient, jwtToken);

        log.info("Building emulator SecretClient -> vaultUrl={}", vaultUrl);
        return new SecretClientBuilder()
                .vaultUrl(vaultUrl)
                .pipeline(pipeline)
                .buildClient();
    }

    private static HttpPipeline buildPipeline(final com.azure.core.http.HttpClient azureHttpClient, final String jwtToken) {
        return new HttpPipelineBuilder()
                .httpClient(azureHttpClient)
                .policies(new RetryPolicy(), new StaticBearerTokenPolicy(jwtToken))
                .build();
    }

    private static String toTokenUrl(final String vaultUrl) {
        return vaultUrl.replaceAll("/+$", "") + TOKEN_PATH;
    }

    static String fetchToken(HttpClient reactorClient, String tokenUrl) {
        final String token = reactorClient
                .get()
                .uri(tokenUrl)
                .responseContent()
                .aggregate()
                .asString()
                .block()
                .trim();
        if (token.isBlank()) {
            throw new IllegalStateException("Emulator returned empty token from " + tokenUrl);
        }
        return token;
    }

    static final class StaticBearerTokenPolicy implements HttpPipelinePolicy {

        private final String headerValue;

        StaticBearerTokenPolicy(String token) {
            this.headerValue = AUTH_HEADER_PREFIX + token;
        }

        @Override
        public Mono<HttpResponse> process(HttpPipelineCallContext context, HttpPipelineNextPolicy next) {
            context.getHttpRequest().getHeaders().set(HttpHeaderName.AUTHORIZATION, headerValue);
            return next.process();
        }

        @Override
        public HttpResponse processSync(HttpPipelineCallContext context, HttpPipelineNextSyncPolicy next) {
            context.getHttpRequest().getHeaders().set(HttpHeaderName.AUTHORIZATION, headerValue);
            return next.processSync();
        }
    }
}
