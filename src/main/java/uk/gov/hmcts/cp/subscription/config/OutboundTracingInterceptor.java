package uk.gov.hmcts.cp.subscription.config;

import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static uk.gov.hmcts.cp.filters.TracingFilter.CORRELATION_ID_HEADER;
import static uk.gov.hmcts.cp.filters.TracingFilter.MDC_CORRELATION_ID;

@Component
public class OutboundTracingInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(final HttpRequest request,
                                        final byte[] body,
                                        final ClientHttpRequestExecution execution) throws IOException {
        final String correlationId = MDC.get(MDC_CORRELATION_ID);
        if (correlationId != null) {
            request.getHeaders().set(CORRELATION_ID_HEADER, correlationId);
        }
        return execution.execute(request, body);
    }
}
