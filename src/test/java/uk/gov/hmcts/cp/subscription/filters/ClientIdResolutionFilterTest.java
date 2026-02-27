package uk.gov.hmcts.cp.subscription.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.hmcts.cp.subscription.config.SubscriptionClientConfig;
import uk.gov.hmcts.cp.subscription.filter.ClientIdResolutionFilter;
import uk.gov.hmcts.cp.subscription.util.JwtTokenParser;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientIdResolutionFilterTest {

    private static final String CLIENT_SUBSCRIPTIONS_PATH = "/client-subscriptions";

    @Mock
    HttpServletRequest httpRequest;
    MockHttpServletResponse httpResponse = new MockHttpServletResponse();
    @Mock
    FilterChain filterChain;
    @Mock
    JwtTokenParser jwtTokenParser;

    private ClientIdResolutionFilter filter;

    @BeforeEach
    void setUp() {
        httpResponse = new MockHttpServletResponse();
        SubscriptionClientConfig configOauthEnabled = new SubscriptionClientConfig(true, "X-Client-Id");
        filter = new ClientIdResolutionFilter(jwtTokenParser, configOauthEnabled);
    }

    @Test
    void path_outside_client_subscriptions_should_not_be_filtered() throws Exception {
        when(httpRequest.getRequestURI()).thenReturn("/actuator/health");
        filter.doFilter(httpRequest, httpResponse, filterChain);
        verify(filterChain).doFilter(httpRequest, httpResponse);
        assertThat(MDC.get(ClientIdResolutionFilter.MDC_CLIENT_ID)).isNull();
    }

    @Test
    void valid_bearer_with_azp_should_put_client_id_in_mdc_and_continue() throws Exception {
        UUID testClientUuid = UUID.fromString("11111111-2222-3333-4444-555555555555");
        when(httpRequest.getRequestURI()).thenReturn(CLIENT_SUBSCRIPTIONS_PATH);
        when(jwtTokenParser.extractClientIdFromToken(httpRequest)).thenReturn(testClientUuid);
        AtomicReference<String> mdcClientId = new AtomicReference<>();
        FilterChain chainThatCapturesMdc = (req, res) -> mdcClientId.set(MDC.get(ClientIdResolutionFilter.MDC_CLIENT_ID));

        filter.doFilter(httpRequest, httpResponse, chainThatCapturesMdc);

        assertThat(mdcClientId.get()).isEqualTo(testClientUuid.toString());
        assertThat(MDC.get(ClientIdResolutionFilter.MDC_CLIENT_ID)).isNull();
    }

    @Test
    void no_client_id_in_token_should_return_401() throws Exception {
        when(httpRequest.getRequestURI()).thenReturn(CLIENT_SUBSCRIPTIONS_PATH);
        when(jwtTokenParser.extractClientIdFromToken(httpRequest)).thenReturn(null);

        filter.doFilter(httpRequest, httpResponse, filterChain);

        assertThat(httpResponse.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(httpRequest, httpResponse);
    }

    @Test
    void oauth_disabled_with_client_id_header_should_put_header_value_in_mdc() throws Exception {
        UUID clientId = UUID.fromString("aaaaaaaa-bbbb-4ccc-dddd-eeeeeeeeeeee");
        when(httpRequest.getRequestURI()).thenReturn(CLIENT_SUBSCRIPTIONS_PATH);
        when(httpRequest.getHeader("X-Client-Id")).thenReturn(clientId.toString());
        SubscriptionClientConfig configOauthDisabled = new SubscriptionClientConfig(false, "X-Client-Id");
        ClientIdResolutionFilter filterOauthDisabled = new ClientIdResolutionFilter(jwtTokenParser, configOauthDisabled);
        AtomicReference<String> mdcClientId = new AtomicReference<>();
        FilterChain chainThatCapturesMdc = (req, res) -> mdcClientId.set(MDC.get(ClientIdResolutionFilter.MDC_CLIENT_ID));

        filterOauthDisabled.doFilter(httpRequest, httpResponse, chainThatCapturesMdc);

        assertThat(mdcClientId.get()).isEqualTo(clientId.toString());
    }

    @Test
    void oauth_disabled_with_header_should_resolve_different_clients_per_request() throws Exception {
        UUID client1 = UUID.fromString("aaaaaaaa-1111-4444-8888-111111111111");
        when(httpRequest.getRequestURI()).thenReturn(CLIENT_SUBSCRIPTIONS_PATH);
        when(httpRequest.getHeader("X-Client-Id")).thenReturn(client1.toString());
        SubscriptionClientConfig configOauthDisabled = new SubscriptionClientConfig(false, "X-Client-Id");
        ClientIdResolutionFilter filterOauthDisabled = new ClientIdResolutionFilter(jwtTokenParser, configOauthDisabled);
        AtomicReference<String> captured = new AtomicReference<>();
        FilterChain chainThatCapturesMdc = (req, res) -> captured.set(MDC.get(ClientIdResolutionFilter.MDC_CLIENT_ID));

        filterOauthDisabled.doFilter(httpRequest, httpResponse, chainThatCapturesMdc);

        assertThat(captured.get()).isEqualTo(client1.toString());

        UUID client2 = UUID.fromString("bbbbbbbb-2222-4444-8888-222222222222");
        when(httpRequest.getHeader("X-Client-Id")).thenReturn(client2.toString());
        captured.set(null);
        filterOauthDisabled.doFilter(httpRequest, httpResponse, chainThatCapturesMdc);
        assertThat(captured.get()).isEqualTo(client2.toString());
    }
}
