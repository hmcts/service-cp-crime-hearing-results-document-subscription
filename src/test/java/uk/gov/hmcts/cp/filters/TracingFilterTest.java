package uk.gov.hmcts.cp.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TracingFilterTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    private final TracingFilter filter = new TracingFilter();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void shouldNotFilter_when_path_not_subscription_or_notifications() {
        when(request.getRequestURI()).thenReturn("/");
        assertThat(filter.shouldNotFilter(request)).isTrue();

        when(request.getRequestURI()).thenReturn("/actuator/health");
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void shouldFilter_when_path_is_client_subscriptions() {
        when(request.getRequestURI()).thenReturn("/client-subscriptions");
        assertThat(filter.shouldNotFilter(request)).isFalse();

        when(request.getRequestURI()).thenReturn("/client-subscriptions/123/documents/456");
        assertThat(filter.shouldNotFilter(request)).isFalse();
    }

    @Test
    void shouldFilter_when_path_is_notifications() {
        when(request.getRequestURI()).thenReturn("/notifications");
        assertThat(filter.shouldNotFilter(request)).isFalse();
    }

    @Test
    void doFilterInternal_puts_correlationId_in_MDC_and_response_when_header_present() throws ServletException, IOException {
        final String correlationId = UUID.randomUUID().toString();
        when(request.getHeader(TracingFilter.CORRELATION_ID_HEADER)).thenReturn(correlationId);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setHeader(TracingFilter.CORRELATION_ID_HEADER, correlationId);
        verify(filterChain).doFilter(request, response);
        assertThat(MDC.get(TracingFilter.MDC_CORRELATION_ID)).isNull();
    }

    @Test
    void doFilterInternal_does_nothing_when_header_absent() throws ServletException, IOException {
        when(request.getHeader(TracingFilter.CORRELATION_ID_HEADER)).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
