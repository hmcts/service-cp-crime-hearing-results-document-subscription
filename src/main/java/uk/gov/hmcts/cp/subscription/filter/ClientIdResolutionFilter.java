package uk.gov.hmcts.cp.subscription.filter;

import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
import static org.springframework.util.ObjectUtils.isEmpty;

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
            response.sendError(ex.getStatusCode().value(), ex.getReason());
        }
    }

    @SuppressWarnings("PMD.OnlyOneReturn")
    private UUID resolveClientId(final HttpServletRequest request) {
        if (config.isOauthEnabled()) {
            log.info("validating clientId");
            final UUID clientId = jwtTokenParser.extractClientIdFromToken(request);
            if (isNull(clientId)) {
                log.error("Subscription request rejected: no client ID in token");
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid authorisation token");
            }
            return clientId;
        }
        log.warn("WARNING clientId authentication is DISABLED");
        final String headerValue = request.getHeader(CLIENT_ID_HEADER);
        if (isEmpty(headerValue)) {
            log.error("Subscription request rejected: empty client ID header");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Empty client ID header");
        }
        return UUID.fromString(headerValue);
    }
}
