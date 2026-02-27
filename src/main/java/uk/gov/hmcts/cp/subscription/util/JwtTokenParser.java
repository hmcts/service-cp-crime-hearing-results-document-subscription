package uk.gov.hmcts.cp.subscription.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.cp.subscription.services.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
public class JwtTokenParser {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AZP_JSON_POINTER = "/azp";
    private static final int MAX_NUMBER_OF_CHUNKS = 2;

    private final JsonMapper jsonMapper;

    public JwtTokenParser(final JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @SuppressWarnings({"PMD.AvoidCatchingGenericException", "PMD.OnlyOneReturn"})
    public UUID extractClientIdFromToken(final HttpServletRequest request) {
        try {
            final String authHeader = nonNull(request) ? request.getHeader("Authorization") : null;
            if (isNull(authHeader) || authHeader.isBlank() || !authHeader.startsWith(BEARER_PREFIX)) {
                return null;
            }
            final String token = authHeader.substring(BEARER_PREFIX.length()).trim();
            if (token.isEmpty()) {
                return null;
            }
            final String[] chunks = token.split("\\.");
            if (chunks.length < MAX_NUMBER_OF_CHUNKS) {
                return null;
            }
            final Base64.Decoder decoder = Base64.getUrlDecoder();
            final String payloadJson = new String(decoder.decode(chunks[1]), StandardCharsets.UTF_8);
            final UUID clientId = jsonMapper.getUUIDAtPath(payloadJson, AZP_JSON_POINTER);
            if (clientId != null) {
                log.debug("Extracted client ID {} from JWT azp claim", clientId);
            }
            return clientId;
        } catch (Exception e) {
            log.warn("Failed to extract client ID from JWT token - returning null. Reason: {}", e.getMessage());
            return null;
        }
    }
}
