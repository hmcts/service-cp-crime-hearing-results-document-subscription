package uk.gov.hmcts.cp.subscription.http;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import uk.gov.hmcts.cp.subscription.http.util.EntraLocal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proves the whole authentication chain against the entra-local container: the emulator registers a
 * subscriber application, issues it a client credentials token, and the service verifies that token
 * by fetching the emulator's JWKS over TLS.
 *
 * <p>Runs against the {@code app-entra} instance, which sets {@code AUTH_MODE=ENFORCE}. The
 * {@code app1} instance the rest of the suite uses runs with validation off and locally minted
 * tokens, so it proves nothing about verification.
 */
@Slf4j
class EntraLocalTokenValidationApiTest {

    private static final String BASE_URL = "http://localhost:8083";
    private static final String SUBSCRIPTION_BODY = """
            {"notificationEndpoint":{"callbackUrl":"https://mycallback"},"eventTypes":["PRISON_COURT_REGISTER_GENERATED"]}
            """;

    private static final EntraLocal ENTRA = new EntraLocal();

    private final RestClient restClient = RestClient.create();

    @BeforeAll
    static void defineApiRoles() {
        ENTRA.defineApiRoles();
    }

    @Test
    void calling_with_a_token_issued_by_entra_local_should_create_a_subscription() {
        final EntraLocal.Subscriber subscriber = ENTRA.registerSubscriber("HRDS Subscriber");

        final var response = restClient.post()
                .uri(BASE_URL + "/client-subscriptions")
                .header(BaseTest.AUTHORIZATION, ENTRA.bearerTokenFor(subscriber))
                .contentType(MediaType.APPLICATION_JSON)
                .body(SUBSCRIPTION_BODY)
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).contains("clientSubscriptionId", "hmac");
    }

    /**
     * The second call conflicts only if the service took the caller's identity from the verified
     * token rather than from anything in the request body.
     */
    @Test
    void subscribing_twice_as_the_same_entra_application_should_conflict_on_the_second_call() {
        final EntraLocal.Subscriber subscriber = ENTRA.registerSubscriber("Repeat Subscriber");
        final String bearerToken = ENTRA.bearerTokenFor(subscriber);

        postSubscription(bearerToken);

        assertThatThrownBy(() -> postSubscription(bearerToken))
                .isInstanceOf(HttpClientErrorException.Conflict.class)
                .hasMessageContaining("subscription already exist");
    }

    @Test
    void calling_with_a_token_minted_for_another_resource_should_be_rejected() {
        final EntraLocal.Subscriber subscriber = ENTRA.registerSubscriber("Wrong Audience Subscriber");
        final String wrongAudienceToken = ENTRA.accessToken(subscriber, EntraLocal.OTHER_APP_ID);

        assertThatThrownBy(() -> postSubscription("Bearer " + wrongAudienceToken))
                .isInstanceOf(HttpClientErrorException.Unauthorized.class);
    }

    @Test
    void calling_without_an_authorization_header_should_be_rejected() {
        assertThatThrownBy(() -> restClient.post()
                .uri(BASE_URL + "/client-subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(SUBSCRIPTION_BODY)
                .retrieve()
                .toBodilessEntity())
                .isInstanceOf(HttpClientErrorException.Unauthorized.class);
    }

    @Test
    void calling_with_a_tampered_token_should_be_rejected() {
        final EntraLocal.Subscriber subscriber = ENTRA.registerSubscriber("Tampered Token Subscriber");
        final String token = ENTRA.accessToken(subscriber, EntraLocal.API_APP_ID);
        final String[] parts = token.split("\\.");
        final String tamperedSignature = parts[2].charAt(0) == 'a' ? "b" + parts[2].substring(1) : "a" + parts[2].substring(1);

        assertThatThrownBy(() -> postSubscription("Bearer " + parts[0] + "." + parts[1] + "." + tamperedSignature))
                .isInstanceOf(HttpClientErrorException.Unauthorized.class);
    }

    @Test
    void a_token_issued_by_entra_local_should_carry_the_claims_a_real_app_only_token_carries() {
        final EntraLocal.Subscriber subscriber = ENTRA.registerSubscriber("Claim Shape Subscriber");

        final JsonNode claims = EntraLocal.claimsOf(ENTRA.accessToken(subscriber, EntraLocal.API_APP_ID));

        assertThat(claims.get("aud").asText()).isEqualTo(EntraLocal.API_APP_ID);
        assertThat(claims.get("azp").asText()).isEqualTo(subscriber.clientId().toString());
        assertThat(claims.get("tid").asText()).isEqualTo(EntraLocal.TENANT_ID);
        assertThat(claims.get("ver").asText()).isEqualTo("2.0");
        assertThat(claims.get("roles")).isNotEmpty();
        assertThat(claims.get("sub").asText()).isEqualTo(claims.get("oid").asText());
        assertThat(claims.get("scp")).isNull();
    }

    private void postSubscription(final String authorizationHeader) {
        restClient.post()
                .uri(BASE_URL + "/client-subscriptions")
                .header(BaseTest.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .body(SUBSCRIPTION_BODY)
                .retrieve()
                .toBodilessEntity();
    }
}
