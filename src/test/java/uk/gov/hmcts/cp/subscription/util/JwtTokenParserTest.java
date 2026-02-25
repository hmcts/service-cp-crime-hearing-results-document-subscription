package uk.gov.hmcts.cp.subscription.util;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.cp.subscription.services.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtTokenParserTest {

    private static final String AZP_UUID = "120d6d4a-d47b-4a40-a782-0065f41de050";

    @Test
    void jwt_token_with_azp_is_extracted() {
        String token = buildTestToken(AZP_UUID);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        JwtTokenParser parser = new JwtTokenParser(new JsonMapper());
        UUID clientId = parser.extractClientIdFromToken(request);

        assertThat(clientId).isEqualTo(UUID.fromString(AZP_UUID));
    }

    private static String buildTestToken(String azpValue) {
        String header = "{\"alg\":\"none\",\"typ\":\"JWT\"}";
        String payload = "{\"azp\":\"" + azpValue + "\"}";
        String headerB64 = Base64.getUrlEncoder().encodeToString(header.getBytes(StandardCharsets.UTF_8));
        String payloadB64 = Base64.getUrlEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return headerB64 + "." + payloadB64 + ".sig";
    }
}
