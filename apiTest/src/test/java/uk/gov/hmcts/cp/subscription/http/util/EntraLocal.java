package uk.gov.hmcts.cp.subscription.http.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.http.HttpClient;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Drives the entra-local container from the test side: register a subscriber application, then
 * exchange its client id and secret for a real access token.
 *
 * <p>The service under test reaches the emulator as {@code entra-local} inside the compose network;
 * these tests reach the same container on the published port, which is why the certificate names
 * both hostnames.
 */
@Slf4j
public final class EntraLocal {

    public static final String TENANT_ID = "11111111-1111-1111-1111-111111111111";

    /** The emulator's seeded local-api application, standing in for this API's own registration. */
    public static final String API_APP_ID = "cccccccc-0000-0000-0000-000000000007";

    /** A seeded application that is not this API — used to mint a token for the wrong audience. */
    public static final String OTHER_APP_ID = "cccccccc-0000-0000-0000-000000000001";

    private static final String BASE_URL = "https://localhost:9443";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RestClient restClient = trustAllRestClient();

    public record Subscriber(UUID clientId, String clientSecret) {
    }

    /**
     * Defines the roles this API recognises on its own registration. The emulator grants a
     * requesting application every role defined on the resource, so this is what puts
     * {@code app.read}/{@code app.write} in an issued token.
     *
     * <p>Real Entra needs an admin-consented app role assignment per client as well, and issues a
     * token with no roles without one.
     */
    public void defineApiRoles() {
        final List<String> defined = existingApiRoles();
        Stream.of("app.read", "app.write")
                .filter(role -> !defined.contains(role))
                .forEach(role -> addRole(API_APP_ID, role));
    }

    public Subscriber registerSubscriber(final String displayName) {
        final JsonNode application = restClient.post()
                .uri(BASE_URL + "/admin/api/apps")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"displayName\":\"" + displayName + "\",\"isConfidential\":true}")
                .retrieve()
                .body(JsonNode.class);
        final String applicationId = application.get("id").asText();

        final JsonNode secret = restClient.post()
                .uri(BASE_URL + "/admin/api/apps/" + applicationId + "/secrets")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"displayName\":\"" + displayName + " secret\"}")
                .retrieve()
                .body(JsonNode.class);

        log.info("Registered subscriber application {} in entra-local", applicationId);
        return new Subscriber(UUID.fromString(applicationId), secret.get("secretText").asText());
    }

    public String accessToken(final Subscriber subscriber, final String resourceAppId) {
        final MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", subscriber.clientId().toString());
        form.add("client_secret", subscriber.clientSecret());
        form.add("scope", resourceAppId + "/.default");

        final JsonNode response = restClient.post()
                .uri(BASE_URL + "/" + TENANT_ID + "/oauth2/v2.0/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(JsonNode.class);
        return response.get("access_token").asText();
    }

    public String bearerTokenFor(final Subscriber subscriber) {
        return "Bearer " + accessToken(subscriber, API_APP_ID);
    }

    @SneakyThrows
    public static JsonNode claimsOf(final String accessToken) {
        final String payload = accessToken.split("\\.")[1];
        return MAPPER.readTree(Base64.getUrlDecoder().decode(payload));
    }

    private List<String> existingApiRoles() {
        final JsonNode application = restClient.get()
                .uri(BASE_URL + "/admin/api/apps/" + API_APP_ID)
                .retrieve()
                .body(JsonNode.class);
        final List<String> roles = new ArrayList<>();
        application.withArray("appRoles").forEach(role -> roles.add(role.get("value").asText()));
        return roles;
    }

    private void addRole(final String applicationId, final String role) {
        restClient.post()
                .uri(BASE_URL + "/admin/api/apps/" + applicationId + "/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"value\":\"" + role + "\",\"displayName\":\"" + role
                        + "\",\"allowedMemberTypes\":[\"Application\"],\"isEnabled\":true}")
                .retrieve()
                .toBodilessEntity();
        log.info("Defined role {} on application {}", role, applicationId);
    }

    @SneakyThrows
    private static RestClient trustAllRestClient() {
        final TrustManager[] trustAll = {new X509TrustManager() {
            @Override
            public void checkClientTrusted(final X509Certificate[] chain, final String authType) {
            }

            @Override
            public void checkServerTrusted(final X509Certificate[] chain, final String authType) {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        }};
        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAll, new java.security.SecureRandom());
        final HttpClient httpClient = HttpClient.newBuilder().sslContext(sslContext).build();
        return RestClient.builder().requestFactory(new JdkClientHttpRequestFactory(httpClient)).build();
    }
}
