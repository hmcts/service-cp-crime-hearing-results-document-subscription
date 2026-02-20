package uk.gov.hmcts.cp.subscription.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class ClientTokenUtil {
    private static final Logger LOG = LoggerFactory.getLogger(ClientTokenUtil.class);

    private static final String BEARER_PREFIX = "Bearer ";

    public static String extractClientIdFromToken(final HttpServletRequest request, final ObjectMapper objectMapper) {
        String result = null;
        final String token = getBearerToken(request);
        if (StringUtils.hasText(token)) {
            final String payload = jwtPayload(token);
            if (StringUtils.hasText(payload)) {
                result = getClientIdFromPayload(payload, objectMapper);
            } else {
                LOG.warn("Invalid or malformed JWT: could not decode payload");
            }
        }
        return result;
    }

    private static String getBearerToken(final HttpServletRequest request) {
        String token = null;
        final String auth = request.getHeader("Authorization");
        if (StringUtils.hasText(auth) && auth.startsWith(BEARER_PREFIX)) {
            final String trimmed = auth.substring(BEARER_PREFIX.length()).trim();
            if (StringUtils.hasText(trimmed)) {
                token = trimmed;
            }
        }
        return token;
    }

    private static String getClientIdFromPayload(final String payload, final ObjectMapper objectMapper) {
        String result = null;
        try {
            final JsonNode azpNode = objectMapper.readTree(payload).path("azp");
            if (azpNode.isTextual()) {
                final String clientId = azpNode.asText().trim();
                if (StringUtils.hasText(clientId)) {
                    result = clientId;
                }
            }
        } catch (IOException e) {
            LOG.warn("Could not parse JWT payload for client ID: {}", e.getMessage());
        }
        return result;
    }

    public static String jwtPayload(final String token) {
        String result = null;
        final String b64Segment = getPayloadSegment(token);
        if (StringUtils.hasText(b64Segment)) {
            result = decodeBase64Url(b64Segment);
        }
        return result;
    }

    private static String getPayloadSegment(final String token) {
        String segment = null;
        if (StringUtils.hasText(token)) {
            final int firstIndex = token.indexOf('.');
            if (firstIndex > 0) {
                final int secondIndex = token.indexOf('.', firstIndex + 1);
                final int endIndex = (secondIndex > firstIndex) ? secondIndex : token.length();
                if (endIndex > firstIndex + 1) {
                    final String b64 = token.substring(firstIndex + 1, endIndex);
                    segment = padBase64Url(b64);
                }
            }
        }
        return segment;
    }

    private static String padBase64Url(final String b64) {
        final int pad = (4 - b64.length() % 4) % 4;
        return (pad == 0) ? b64 : b64 + "====".substring(0, pad);
    }

    private static String decodeBase64Url(final String b64) {
        String result = null;
        try {
            result = new String(Base64.getUrlDecoder().decode(b64), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            LOG.debug("Failed to decode JWT payload base64: {}", e.getMessage());
        }
        return result;
    }
}
