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
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class TracingFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_KEY = "X-Correlation-Id";

    @Override
    protected boolean shouldNotFilter(@Nonnull final HttpServletRequest request) {
        return false;
    }

    @Override
    protected void doFilterInternal(@Nonnull final HttpServletRequest request,
                                    @Nonnull final HttpServletResponse response,
                                    @Nonnull final FilterChain filterChain) throws ServletException, IOException {
        try {
            final String correlationId = getCorrelationId(request);
            MDC.put(CORRELATION_ID_KEY, getCorrelationId(request));
            response.setHeader(CORRELATION_ID_KEY, correlationId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CORRELATION_ID_KEY);
        }
    }

    private String getCorrelationId(final HttpServletRequest request) {
        return request.getHeader(CORRELATION_ID_KEY) == null
                ? UUID.randomUUID().toString()
                : request.getHeader(CORRELATION_ID_KEY);
    }
}
