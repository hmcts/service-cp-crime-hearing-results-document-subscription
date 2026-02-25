package uk.gov.hmcts.cp.subscription.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import uk.gov.hmcts.cp.subscription.util.JwtTokenParser;

import java.io.IOException;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class ClientIdResolutionFilter extends OncePerRequestFilter {

    public static final String RESOLVED_CLIENT_ID = "resolvedClientId";

    private final JwtTokenParser jwtTokenParser;

    @Value("${subscription.oauth-enabled:true}")
    private boolean oauthEnabled;

    @Value("${subscription.default-client-id:}")
    private String defaultClientId;

    public static UUID getResolvedClientId(final HttpServletRequest request) {
        return (UUID) request.getAttribute(RESOLVED_CLIENT_ID);
    }

    @Override
    protected boolean shouldNotFilter(@Nonnull final HttpServletRequest request) {
        final String path = request.getRequestURI();
        return !path.startsWith("/client-subscriptions");
    }

    @Override
    protected void doFilterInternal(@Nonnull final HttpServletRequest request,
                                    @Nonnull final HttpServletResponse response,
                                    @Nonnull final FilterChain filterChain) throws ServletException, IOException {

        final UUID clientId;
        if (oauthEnabled) {
            clientId = jwtTokenParser.extractClientIdFromToken(request);
            if (clientId == null) {
                log.warn("Subscription request rejected: no client ID in token");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        } else {
            clientId = UUID.fromString(defaultClientId);
        }
        request.setAttribute(RESOLVED_CLIENT_ID, clientId);
        filterChain.doFilter(request, response);
    }
}
