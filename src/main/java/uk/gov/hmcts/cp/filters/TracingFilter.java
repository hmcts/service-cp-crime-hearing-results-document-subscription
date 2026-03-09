package uk.gov.hmcts.cp.filters;

import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class TracingFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String MDC_CORRELATION_ID = "correlationId";

    private static final String CLIENT_SUBSCRIPTIONS_PREFIX = "/client-subscriptions";
    private static final String NOTIFICATIONS_PREFIX = "/notifications";

    @Override
    protected boolean shouldNotFilter(@Nonnull final HttpServletRequest request) {
        final String uri = request.getRequestURI();
        return !uri.startsWith(CLIENT_SUBSCRIPTIONS_PREFIX) && !uri.startsWith(NOTIFICATIONS_PREFIX);
    }

    @Override
    protected void doFilterInternal(@Nonnull final HttpServletRequest request,
                                    @Nonnull final HttpServletResponse response,
                                    @Nonnull final FilterChain filterChain) throws ServletException, IOException {
        try {
            final String correlationId = request.getHeader(CORRELATION_ID_HEADER);
            if (correlationId != null) {
                MDC.put(MDC_CORRELATION_ID, correlationId);
                response.setHeader(CORRELATION_ID_HEADER, correlationId);
            }
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_CORRELATION_ID);
        }
    }
}
