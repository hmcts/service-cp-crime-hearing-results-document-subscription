package uk.gov.hmcts.cp.subscription.integration.helpers;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class JwtHelper {

    private JwtHelper() {
    }

    public static String bearerTokenWithAzp(String clientId) {
        return "Bearer " + jwtWithClaims("{\"azp\":\"" + clientId + "\"}");
    }

    public static String jwtWithClaims(String claimsJson) {
        String header = "{\"alg\":\"RS256\",\"typ\":\"JWT\"}";
        String headerB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(header.getBytes(StandardCharsets.UTF_8));
        String payloadB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(claimsJson.getBytes(StandardCharsets.UTF_8));
        return headerB64 + "." + payloadB64 + ".sig";
    }
}
