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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
    void shouldAlwaysFilter_all_paths() {
        assertThat(filter.shouldNotFilter(request)).isFalse();
    }

    @Test
    void doFilterInternal_puts_correlationId_in_MDC_and_response_when_header_present() throws ServletException, IOException {
        final String correlationId = UUID.randomUUID().toString();
        when(request.getHeader(TracingFilter.CORRELATION_ID_KEY)).thenReturn(correlationId);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setHeader(TracingFilter.CORRELATION_ID_KEY, correlationId);
        verify(filterChain).doFilter(request, response);
        assertThat(MDC.get(TracingFilter.CORRELATION_ID_KEY)).isNull();
    }

    @Test
    void doFilterInternal_generates_correlationId_when_header_absent() throws ServletException, IOException {
        when(request.getHeader(TracingFilter.CORRELATION_ID_KEY)).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setHeader(eq(TracingFilter.CORRELATION_ID_KEY), anyString());
        verify(filterChain).doFilter(request, response);
    }
}
