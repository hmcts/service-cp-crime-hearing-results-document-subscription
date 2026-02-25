package uk.gov.hmcts.cp.subscription.util;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.cp.subscription.services.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenParserTest {

    private static final String AZP_UUID = "120d6d4a-d47b-4a40-a782-0065f41de050";

    @Test
    void jwt_token_with_azp_is_extracted() {
        String token = buildTestToken(AZP_UUID);
        JsonMapper jsonMapper = new JsonMapper();
        String[] chunks = token.split("\\.");
        Base64.Decoder decoder = Base64.getUrlDecoder();
        String payloadJson = new String(decoder.decode(chunks[1]), StandardCharsets.UTF_8);

        UUID clientId = jsonMapper.getUUIDAtPath(payloadJson, "/azp");

        assertThat(clientId).isEqualTo(UUID.fromString(AZP_UUID));
    }

    private static String buildTestToken(String azpValue) {
        String header = "{\"alg\":\"none\",\"typ\":\"JWT\"}";
        String payload = "{\"azp\":\"" + azpValue + "\"}";
        String headerB64 = base64UrlEncode(header);
        String payloadB64 = base64UrlEncode(payload);
        return headerB64 + "." + payloadB64 + ".sig";
    }

    private static String base64UrlEncode(String s) {
        String b64 = Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8));
        int pad = (4 - b64.length() % 4) % 4;
        return pad == 0 ? b64 : b64 + "====".substring(0, pad);
    }
}
