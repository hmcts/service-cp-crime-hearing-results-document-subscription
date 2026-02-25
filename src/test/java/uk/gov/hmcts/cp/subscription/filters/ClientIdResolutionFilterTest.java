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
import uk.gov.hmcts.cp.subscription.util.JwtTokenParser;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientIdResolutionFilterTest {

    private static final String CLIENT_SUBSCRIPTIONS_PATH = "/client-subscriptions";

    @Mock
    HttpServletRequest httpRequest;
    @Mock
    HttpServletResponse httpResponse;
    @Mock
    FilterChain filterChain;
    @Mock
    JwtTokenParser jwtTokenParser;

    private ClientIdResolutionFilter filter;

    @BeforeEach
    void setUp() throws Exception {
        filter = new ClientIdResolutionFilter(jwtTokenParser);
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
        UUID testClientUuid = UUID.fromString("11111111-2222-3333-4444-555555555555");
        when(httpRequest.getRequestURI()).thenReturn(CLIENT_SUBSCRIPTIONS_PATH);
        ReflectionTestUtils.setField(filter, "oauthEnabled", true);
        when(jwtTokenParser.extractClientIdFromToken(httpRequest)).thenReturn(testClientUuid);
        filter.doFilter(httpRequest, httpResponse, filterChain);
        verify(httpRequest).setAttribute(ClientIdResolutionFilter.RESOLVED_CLIENT_ID, testClientUuid);
        verify(filterChain).doFilter(httpRequest, httpResponse);
    }

    @Test
    void no_bearer_token_should_return_401() throws Exception {
        when(httpRequest.getRequestURI()).thenReturn(CLIENT_SUBSCRIPTIONS_PATH);
        ReflectionTestUtils.setField(filter, "oauthEnabled", true);
        when(jwtTokenParser.extractClientIdFromToken(httpRequest)).thenReturn((UUID) null);
        filter.doFilter(httpRequest, httpResponse, filterChain);
        verify(httpResponse).setStatus(401);
        verify(filterChain, never()).doFilter(httpRequest, httpResponse);
    }

    @Test
    void token_without_azp_should_return_401() throws Exception {
        when(httpRequest.getRequestURI()).thenReturn(CLIENT_SUBSCRIPTIONS_PATH);
        ReflectionTestUtils.setField(filter, "oauthEnabled", true);
        when(jwtTokenParser.extractClientIdFromToken(httpRequest)).thenReturn((UUID) null);
        filter.doFilter(httpRequest, httpResponse, filterChain);
        verify(httpResponse).setStatus(401);
        verify(filterChain, never()).doFilter(httpRequest, httpResponse);
    }

    @Test
    void oauth_disabled_with_default_should_use_default_client_id() throws Exception {
        UUID defaultUuid = UUID.fromString("aaaaaaaa-bbbb-4ccc-dddd-eeeeeeeeeeee");
        when(httpRequest.getRequestURI()).thenReturn(CLIENT_SUBSCRIPTIONS_PATH);
        ReflectionTestUtils.setField(filter, "oauthEnabled", false);
        ReflectionTestUtils.setField(filter, "defaultClientId", defaultUuid.toString());
        filter.doFilter(httpRequest, httpResponse, filterChain);
        verify(httpRequest).setAttribute(ClientIdResolutionFilter.RESOLVED_CLIENT_ID, defaultUuid);
        verify(filterChain).doFilter(httpRequest, httpResponse);
    }
}
