package uk.gov.hmcts.cp.subscription.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.cp.subscription.config.SubscriptionClientConfig;
import uk.gov.hmcts.cp.subscription.util.JwtTokenParser;

import java.io.IOException;
import java.util.UUID;

import static java.util.Objects.isNull;

@Component
@Slf4j
public class ClientIdResolutionFilter extends OncePerRequestFilter {

    public static final String MDC_CLIENT_ID = "clientId";

    private static final String CLIENT_SUBSCRIPTIONS_PREFIX = "/client-subscriptions";

    private final JwtTokenParser jwtTokenParser;
    private final boolean oauthEnabled;
    private final String clientIdHeaderName;

    public ClientIdResolutionFilter(final JwtTokenParser jwtTokenParser, final SubscriptionClientConfig config) {
        this.jwtTokenParser = jwtTokenParser;
        this.oauthEnabled = config.isOauthEnabled();
        this.clientIdHeaderName = config.getClientIdHeaderName().trim();
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
            response.sendError(ex.getStatusCode().value(), ex.getReason());
        }
    }

    private UUID resolveClientId(final HttpServletRequest request) {
        if (oauthEnabled) {
            final UUID clientId = jwtTokenParser.extractClientIdFromToken(request);
            if (isNull(clientId)) {
                log.warn("Subscription request rejected: no client ID in token");
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid authorisation token");
            }
            return clientId;
        }
        final String headerValue = request.getHeader(clientIdHeaderName);
        if (isNull(headerValue) || headerValue.isBlank()) {
            log.warn("Subscription request rejected: missing or blank client ID header");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing client ID header");
        }
        return UUID.fromString(headerValue.trim());
    }
}
