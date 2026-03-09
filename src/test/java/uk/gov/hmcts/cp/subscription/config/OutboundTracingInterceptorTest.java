package uk.gov.hmcts.cp.subscription.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import uk.gov.hmcts.cp.filters.TracingFilter;

import java.net.URI;
import java.util.HashMap;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OutboundTracingInterceptorTest {

    private final OutboundTracingInterceptor interceptor = new OutboundTracingInterceptor();

    @BeforeEach
    void setUp() {
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void intercept_adds_X_Correlation_Id_from_MDC() throws Exception {
        final String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);

        final HttpHeaders headers = new HttpHeaders();
        final org.springframework.http.HttpRequest request = mock(org.springframework.http.HttpRequest.class);
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getURI()).thenReturn(URI.create("https://example.com/callback"));
        when(request.getHeaders()).thenReturn(headers);
        when(request.getAttributes()).thenReturn(new HashMap<>());

        final ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(request, new byte[0])).thenReturn(mock(ClientHttpResponse.class));

        interceptor.intercept(request, new byte[0], execution);

        assertThat(request.getHeaders().getFirst(TracingFilter.CORRELATION_ID_HEADER)).isEqualTo(correlationId);
    }

    @Test
    void intercept_does_not_add_header_when_MDC_correlationId_absent() throws Exception {
        final HttpHeaders headers = new HttpHeaders();
        final org.springframework.http.HttpRequest request = mock(org.springframework.http.HttpRequest.class);
        when(request.getMethod()).thenReturn(HttpMethod.POST);
        when(request.getURI()).thenReturn(URI.create("https://example.com/notify"));
        when(request.getHeaders()).thenReturn(headers);
        when(request.getAttributes()).thenReturn(new HashMap<>());

        final ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(request, new byte[0])).thenReturn(mock(ClientHttpResponse.class));

        interceptor.intercept(request, new byte[0], execution);

        assertThat(request.getHeaders().getFirst(TracingFilter.CORRELATION_ID_HEADER)).isNull();
    }
}
