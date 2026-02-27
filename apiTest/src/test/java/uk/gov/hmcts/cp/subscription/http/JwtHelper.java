package uk.gov.hmcts.cp.subscription.http;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

final class JwtHelper {

    private JwtHelper() {
    }

    static String bearerTokenWithAzp(final String clientId) {
        final String header = "{\"alg\":\"RS256\",\"typ\":\"JWT\"}";
        final String payload = "{\"azp\":\"" + clientId + "\"}";
        final String headerB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(header.getBytes(StandardCharsets.UTF_8));
        final String payloadB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return "Bearer " + headerB64 + "." + payloadB64 + ".sig";
    }
}
