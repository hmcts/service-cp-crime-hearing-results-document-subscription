package uk.gov.hmcts.cp.subscription.util;

import jakarta.servlet.http.HttpServletRequest;
import uk.gov.hmcts.cp.subscription.services.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

public class JwtTokenParser {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AZP_JSON_POINTER = "/azp";

    private final JsonMapper jsonMapper;

    public JwtTokenParser(final JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public UUID extractClientIdFromToken(final HttpServletRequest request) {
        final String token = request.getHeader("Authorization").substring(BEARER_PREFIX.length()).trim();
        final String[] chunks = token.split("\\.");
        final Base64.Decoder decoder = Base64.getUrlDecoder();
        final String payloadJson = new String(decoder.decode(chunks[1]), StandardCharsets.UTF_8);
        return jsonMapper.getUUIDAtPath(payloadJson, AZP_JSON_POINTER);
    }
}
