package uk.gov.hmcts.cp.subscription.filter;

import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.cp.subscription.config.SubscriptionClientConfig;
import uk.gov.hmcts.cp.subscription.util.JwtTokenParser;

import java.io.IOException;
import java.util.UUID;

@Component
@Slf4j
public class ClientIdResolutionFilter extends OncePerRequestFilter {

    public static final String MDC_CLIENT_ID = "clientId";

    private static final String CLIENT_SUBSCRIPTIONS_PREFIX = "/client-subscriptions";
    private static final String CLIENT_ID_HEADER = "X-Client-Id";

    private final JwtTokenParser jwtTokenParser;
    private final SubscriptionClientConfig config;

    public ClientIdResolutionFilter(final JwtTokenParser jwtTokenParser, final SubscriptionClientConfig config) {
        this.jwtTokenParser = jwtTokenParser;
        this.config = config;
    }

    @Override
    protected boolean shouldNotFilter(@Nonnull final HttpServletRequest request) {
        return !request.getRequestURI().startsWith(CLIENT_SUBSCRIPTIONS_PREFIX);
    }

    @Override
    protected void doFilterInternal(@Nonnull final HttpServletRequest request,
                                    @Nonnull final HttpServletResponse response,
                                    @Nonnull final FilterChain filterChain) throws ServletException, IOException {
        try {
            final UUID clientId = resolveClientId(request);
            MDC.put(MDC_CLIENT_ID, clientId.toString());
            try {
                filterChain.doFilter(request, response);
            } finally {
                MDC.remove(MDC_CLIENT_ID);
            }
        } catch (ResponseStatusException ex) {
            response.sendError(ex.getStatusCode().value(), ex.getReason() != null ? ex.getReason() : ex.getMessage());
        } catch (HttpClientErrorException ex) {
            response.sendError(ex.getStatusCode().value(), ex.getStatusText());
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid client ID: {}", ex.getMessage());
                response.sendError(401, "Missing or invalid client ID");
        }
    }

    private UUID resolveClientId(final HttpServletRequest request) {
        log.info("validating clientId");
        return config.isOauthEnabled() ?
                getClientIdFromJwtToken(request) :
                getClientIdFromHeader(request);
    }

    private UUID getClientIdFromJwtToken(final HttpServletRequest request) {
        log.info("Extracting clientId from JwtToken");
        return jwtTokenParser.extractClientIdFromToken(request);
    }

    private UUID getClientIdFromHeader(final HttpServletRequest request) {
        log.warn("WARNING clientId authentication is DISABLED. Taking clientId from header");
        return UUID.fromString(request.getHeader(CLIENT_ID_HEADER));
    }
}
