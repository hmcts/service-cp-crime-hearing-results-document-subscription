package uk.gov.hmcts.cp.subscription.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientTokenUtilTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    private HttpServletRequest request;

    @Test
    void extractClientIdFromToken_should_return_null_when_authorization_header_missing() {
        when(request.getHeader("Authorization")).thenReturn(null);

        assertThat(ClientTokenUtil.extractClientIdFromToken(request, OBJECT_MAPPER)).isNull();
    }

    @Test
    void extractClientIdFromToken_should_return_null_when_authorization_header_empty() {
        when(request.getHeader("Authorization")).thenReturn("");

        assertThat(ClientTokenUtil.extractClientIdFromToken(request, OBJECT_MAPPER)).isNull();
    }

    @Test
    void extractClientIdFromToken_should_return_null_when_not_bearer() {
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        assertThat(ClientTokenUtil.extractClientIdFromToken(request, OBJECT_MAPPER)).isNull();
    }

    @Test
    void extractClientIdFromToken_should_return_null_when_bearer_with_no_token() {
        when(request.getHeader("Authorization")).thenReturn("Bearer ");

        assertThat(ClientTokenUtil.extractClientIdFromToken(request, OBJECT_MAPPER)).isNull();
    }

    @Test
    void extractClientIdFromToken_should_return_null_when_bearer_with_whitespace_only() {
        when(request.getHeader("Authorization")).thenReturn("Bearer   ");

        assertThat(ClientTokenUtil.extractClientIdFromToken(request, OBJECT_MAPPER)).isNull();
    }

    @Test
    void extractClientIdFromToken_should_return_client_id_when_valid_token_with_azp() {
        String token = jwtWithPayload("{\"azp\":\"my-client-id\"}");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        assertThat(ClientTokenUtil.extractClientIdFromToken(request, OBJECT_MAPPER)).isEqualTo("my-client-id");
    }

    @Test
    void extractClientIdFromToken_should_return_trimmed_client_id() {
        String token = jwtWithPayload("{\"azp\":\"  client-123  \"}");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        assertThat(ClientTokenUtil.extractClientIdFromToken(request, OBJECT_MAPPER)).isEqualTo("client-123");
    }

    @Test
    void extractClientIdFromToken_should_return_null_when_azp_missing() {
        String token = jwtWithPayload("{\"sub\":\"user-id\"}");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        assertThat(ClientTokenUtil.extractClientIdFromToken(request, OBJECT_MAPPER)).isNull();
    }

    @Test
    void extractClientIdFromToken_should_return_null_when_azp_empty_string() {
        String token = jwtWithPayload("{\"azp\":\"\"}");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        assertThat(ClientTokenUtil.extractClientIdFromToken(request, OBJECT_MAPPER)).isNull();
    }

    @Test
    void extractClientIdFromToken_should_return_null_when_azp_not_string() {
        String token = jwtWithPayload("{\"azp\":123}");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        assertThat(ClientTokenUtil.extractClientIdFromToken(request, OBJECT_MAPPER)).isNull();
    }

    @Test
    void extractClientIdFromToken_should_return_null_when_token_malformed() {
        when(request.getHeader("Authorization")).thenReturn("Bearer not.three.parts");

        assertThat(ClientTokenUtil.extractClientIdFromToken(request, OBJECT_MAPPER)).isNull();
    }

    @Test
    void extractClientIdFromToken_should_return_null_when_payload_invalid_base64() {
        when(request.getHeader("Authorization")).thenReturn("Bearer a.invalid!!!base64.sig");

        assertThat(ClientTokenUtil.extractClientIdFromToken(request, OBJECT_MAPPER)).isNull();
    }

    @Test
    void extractClientIdFromToken_should_return_null_when_payload_not_json() {
        String notJson = Base64.getUrlEncoder().withoutPadding().encodeToString("not json".getBytes(StandardCharsets.UTF_8));
        when(request.getHeader("Authorization")).thenReturn("Bearer a." + notJson + ".sig");

        assertThat(ClientTokenUtil.extractClientIdFromToken(request, OBJECT_MAPPER)).isNull();
    }

    @Test
    void jwtPayload_should_return_null_when_token_null() {
        assertThat(ClientTokenUtil.jwtPayload(null)).isNull();
    }

    @Test
    void jwtPayload_should_return_null_when_token_empty() {
        assertThat(ClientTokenUtil.jwtPayload("")).isNull();
    }

    @Test
    void jwtPayload_should_return_null_when_token_whitespace_only() {
        assertThat(ClientTokenUtil.jwtPayload("   ")).isNull();
    }

    @Test
    void jwtPayload_should_return_null_when_no_dots() {
        assertThat(ClientTokenUtil.jwtPayload("nodots")).isNull();
    }

    @Test
    void jwtPayload_should_return_decoded_payload_for_valid_jwt() {
        String payloadJson = "{\"azp\":\"client-1\"}";
        String token = jwtWithPayload(payloadJson);

        assertThat(ClientTokenUtil.jwtPayload(token)).isEqualTo(payloadJson);
    }

    @Test
    void jwtPayload_should_handle_padding_required() {
        String payloadJson = "{\"a\":1}";
        String payloadB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        String token = "header." + payloadB64 + ".sig";

        assertThat(ClientTokenUtil.jwtPayload(token)).isEqualTo(payloadJson);
    }

    @Test
    void jwtPayload_should_return_null_when_middle_segment_invalid_base64() {
        String token = "header.not!!!valid.base64.sig";

        assertThat(ClientTokenUtil.jwtPayload(token)).isNull();
    }

    private static String jwtWithPayload(String payloadJson) {
        String header = "{\"alg\":\"RS256\",\"typ\":\"JWT\"}";
        String headerB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(header.getBytes(StandardCharsets.UTF_8));
        String payloadB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        return headerB64 + "." + payloadB64 + ".sig";
    }
}
