package uk.gov.hmcts.cp.subscription.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import uk.gov.hmcts.cp.subscription.services.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
@RequiredArgsConstructor
public class JwtTokenParser {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AZP_JSON_POINTER = "/azp";
    private static final int MAX_NUMBER_OF_CHUNKS = 2;

    private final JsonMapper jsonMapper;

    @SuppressWarnings({"PMD.AvoidCatchingGenericException", "PMD.OnlyOneReturn"})
    public UUID extractClientIdFromToken(final HttpServletRequest request) {
        try {
            final String authHeader = nonNull(request) ? request.getHeader("Authorization") : null;
            if (isNull(authHeader) || authHeader.isBlank() || !authHeader.startsWith(BEARER_PREFIX)) {
                throw throwError("Authorization header is not set or does not start with " + BEARER_PREFIX);
            }
            final String token = authHeader.substring(BEARER_PREFIX.length()).trim();
            if (token.isEmpty()) {
                throw throwError("Authorization token is not set");
            }
            final String[] chunks = token.split("\\.");
            if (chunks.length < MAX_NUMBER_OF_CHUNKS) {
                throw throwError("Authorization token has " + chunks.length
                        + " period-separated chunks, expected at least " + MAX_NUMBER_OF_CHUNKS);
            }
            final Base64.Decoder decoder = Base64.getUrlDecoder();
            final String payloadJson = new String(decoder.decode(chunks[1]), StandardCharsets.UTF_8);
            final UUID clientId = jsonMapper.getUUIDAtPath(payloadJson, AZP_JSON_POINTER);
            if (clientId != null) {
                log.info("Authorization extracted client ID {} from JWT azp claim", clientId);
                return clientId;
            }
            throw throwError("Authorization azp claim missing or not a valid UUID");
        } catch (HttpClientErrorException e) {
            throw e;
        } catch (Exception e) {
            throw throwError("Authorization failed to extract client ID from JWT token. Exception: " + e.getMessage());
        }
    }

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private HttpClientErrorException throwError(final String errorMessage) {
        log.error("Authorization token error. {}", errorMessage);
        return new HttpClientErrorException(HttpStatus.UNAUTHORIZED, errorMessage);
    }
}
