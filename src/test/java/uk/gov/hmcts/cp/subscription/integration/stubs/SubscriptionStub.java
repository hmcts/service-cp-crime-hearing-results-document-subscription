package uk.gov.hmcts.cp.subscription.integration.stubs;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.gov.hmcts.cp.subscription.integration.helpers.JwtHelper;
import uk.gov.hmcts.cp.subscription.services.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public final class SubscriptionStub {

    private static final String TEST_CLIENT_ID = "11111111-2222-3333-4444-555555555555";
    private static final String SUBSCRIPTION_PCR_REQUEST_PATH = "stubs/requests/subscription/subscription-pcr-request.json";
    private static final String PLACEHOLDER_CALLBACK_URL = "{{callback.url}}";
    public static final String CLIENT_SUBSCRIPTION_ID_FIELD = "clientSubscriptionId";
    public static JsonMapper jsonMapper = new JsonMapper();

    public static String createSubscriptionPcr(MockMvc mockMvc, String clientSubscriptionsUri,
                                               String callbackBaseUrl, String callbackUri) throws Exception {
        return createSubscriptionPcr(mockMvc, clientSubscriptionsUri, callbackBaseUrl, callbackUri, TEST_CLIENT_ID);
    }

    public static String createSubscriptionPcr(MockMvc mockMvc, String clientSubscriptionsUri,
                                               String callbackBaseUrl, String callbackUri,
                                               String clientId) throws Exception {
        String callbackUrl = callbackBaseUrl.endsWith("/")
                ? callbackBaseUrl + callbackUri.substring(1)
                : callbackBaseUrl + callbackUri;
        String requestBody = loadPayload(SUBSCRIPTION_PCR_REQUEST_PATH).replace(PLACEHOLDER_CALLBACK_URL, callbackUrl);
        String responseBody = postSubscriptionAndReturnJson(mockMvc, clientSubscriptionsUri, requestBody, clientId);
        log.info("got keyId:{}", jsonMapper.getStringAtPath(responseBody, "/hmac/keyId"));
        log.info("got secret:{}", jsonMapper.getStringAtPath(responseBody, "/hmac/secret"));
        log.info("got clientSubscriptionId:{}", jsonMapper.getUUIDAtPath(responseBody, "/clientSubscriptionId"));
        return responseBody;
    }

    public static UUID createSubscriptionFromPayload(MockMvc mockMvc, String clientSubscriptionsUri,
                                                     String callbackBaseUrl, String callbackUri,
                                                     String payloadWithPlaceholder, String clientId) throws Exception {
        String callbackUrl = callbackBaseUrl.endsWith("/")
                ? callbackBaseUrl + callbackUri.substring(1)
                : callbackBaseUrl + callbackUri;
        String body = payloadWithPlaceholder.replace(PLACEHOLDER_CALLBACK_URL, callbackUrl);
        String json = postSubscriptionAndReturnJson(mockMvc, clientSubscriptionsUri, body, clientId);
        return extractClientSubscriptionId(json);
    }

    public static String postSubscriptionAndReturnJson(MockMvc mockMvc, String clientSubscriptionsUri,
                                                       String body, String clientId) throws Exception {
        return mockMvc.perform(post(clientSubscriptionsUri)
                        .header("Authorization", JwtHelper.bearerTokenWithAzp(clientId))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$." + CLIENT_SUBSCRIPTION_ID_FIELD).exists())
                .andReturn().getResponse().getContentAsString();
    }

    public static ResultActions deleteSubscription(MockMvc mockMvc, String clientSubscriptionsUri,
                                                   UUID clientSubscriptionId) throws Exception {
        return deleteSubscription(mockMvc, clientSubscriptionsUri, clientSubscriptionId, TEST_CLIENT_ID);
    }

    public static ResultActions deleteSubscription(MockMvc mockMvc, String clientSubscriptionsUri,
                                                   UUID clientSubscriptionId, String clientId) throws Exception {
        return mockMvc.perform(delete(clientSubscriptionsUri + "/{clientSubscriptionId}", clientSubscriptionId)
                .header("Authorization", JwtHelper.bearerTokenWithAzp(clientId)));
    }

    private static String loadPayload(String path) throws IOException {
        return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
    }

    private static UUID extractClientSubscriptionId(String json) throws IOException {
        JsonNode node = new JsonMapper().toJsonNode(json);
        return UUID.fromString(node.get(CLIENT_SUBSCRIPTION_ID_FIELD).asText());
    }
}
