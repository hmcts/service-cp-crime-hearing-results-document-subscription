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
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import uk.gov.hmcts.cp.subscription.util.JwtTokenParser;

import java.io.IOException;

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

    public static String getResolvedClientId(final HttpServletRequest request) {
        return (String) request.getAttribute(RESOLVED_CLIENT_ID);
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

        final String clientId;
        if (oauthEnabled) {
            clientId = jwtTokenParser.extractClientIdFromToken(request);
            if (!StringUtils.hasText(clientId)) {
                log.warn("Subscription request rejected: no client ID in token");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        } else {
            clientId = defaultClientId;
        }
        request.setAttribute(RESOLVED_CLIENT_ID, clientId);
        filterChain.doFilter(request, response);
    }
}
