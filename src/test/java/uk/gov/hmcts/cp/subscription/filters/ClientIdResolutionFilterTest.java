package uk.gov.hmcts.cp.subscription.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.cp.subscription.filter.ClientIdResolutionFilter;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.cp.subscription.integration.helpers.JwtHelper.jwtWithClaims;

@ExtendWith(MockitoExtension.class)
class ClientIdResolutionFilterTest {

    private static final String CLIENT_SUBSCRIPTIONS_PATH = "/client-subscriptions";

    @Mock
    HttpServletRequest httpRequest;
    @Mock
    HttpServletResponse httpResponse;
    @Mock
    FilterChain filterChain;

    private ClientIdResolutionFilter filter;

    @BeforeEach
    void setUp() throws Exception {
        filter = new ClientIdResolutionFilter();
    }

    @Test
    void path_outside_client_subscriptions_should_not_be_filtered() throws Exception {
        when(httpRequest.getRequestURI()).thenReturn("/actuator/health");
        filter.doFilter(httpRequest, httpResponse, filterChain);
        verify(filterChain).doFilter(httpRequest, httpResponse);
        verify(httpRequest, never()).setAttribute(anyString(), anyString());
    }

    @Test
    void valid_bearer_with_azp_should_set_client_id_and_continue() throws Exception {
        when(httpRequest.getRequestURI()).thenReturn(CLIENT_SUBSCRIPTIONS_PATH);
        ReflectionTestUtils.setField(filter, "oauthEnabled", true);
        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer " + jwtWithClaims("{\"azp\":\"test-client-id\"}"));
        filter.doFilter(httpRequest, httpResponse, filterChain);
        verify(httpRequest).setAttribute(ClientIdResolutionFilter.RESOLVED_CLIENT_ID, "test-client-id");
        verify(filterChain).doFilter(httpRequest, httpResponse);
    }

    @Test
    void no_bearer_token_should_return_401() throws Exception {
        when(httpRequest.getRequestURI()).thenReturn(CLIENT_SUBSCRIPTIONS_PATH);
        ReflectionTestUtils.setField(filter, "oauthEnabled", true);
        when(httpRequest.getHeader("Authorization")).thenReturn(null);
        filter.doFilter(httpRequest, httpResponse, filterChain);
        verify(httpResponse).setStatus(401);
        verify(filterChain, never()).doFilter(httpRequest, httpResponse);
    }

    @Test
    void token_without_azp_should_return_401() throws Exception {
        when(httpRequest.getRequestURI()).thenReturn(CLIENT_SUBSCRIPTIONS_PATH);
        ReflectionTestUtils.setField(filter, "oauthEnabled", true);
        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer " + jwtWithClaims("{\"sub\":\"other\"}"));
        filter.doFilter(httpRequest, httpResponse, filterChain);
        verify(httpResponse).setStatus(401);
        verify(filterChain, never()).doFilter(httpRequest, httpResponse);
    }

    @Test
    void oauth_disabled_with_default_should_use_default_client_id() throws Exception {
        when(httpRequest.getRequestURI()).thenReturn(CLIENT_SUBSCRIPTIONS_PATH);
        ReflectionTestUtils.setField(filter, "oauthEnabled", false);
        ReflectionTestUtils.setField(filter, "defaultClientId", "local-client");
        filter.doFilter(httpRequest, httpResponse, filterChain);
        verify(httpRequest).setAttribute(ClientIdResolutionFilter.RESOLVED_CLIENT_ID, "local-client");
        verify(filterChain).doFilter(httpRequest, httpResponse);
    }
}
