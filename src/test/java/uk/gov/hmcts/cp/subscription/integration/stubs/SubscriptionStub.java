package uk.gov.hmcts.cp.subscription.integration.stubs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public final class SubscriptionStub {

    private static final String SUBSCRIPTION_PCR_REQUEST_PATH = "stubs/requests/subscription/subscription-pcr-request.json";
    private static final String SUBSCRIPTION_CUSTODIAL_ONLY_PATH = "stubs/requests/subscription/subscription-custodial-only.json";
    private static final String PLACEHOLDER_CALLBACK_URL = "{{callback.url}}";
    public static final String CLIENT_SUBSCRIPTION_ID = "clientSubscriptionId";

    public static UUID createSubscriptionPcr(MockMvc mockMvc, String clientSubscriptionsUri,
                                             String callbackBaseUrl, String callbackUri) throws Exception {
        String callbackUrl = callbackBaseUrl.endsWith("/")
                ? callbackBaseUrl + callbackUri.substring(1)
                : callbackBaseUrl + callbackUri;
        String body = loadPayload(SUBSCRIPTION_PCR_REQUEST_PATH).replace(PLACEHOLDER_CALLBACK_URL, callbackUrl);
        String json = postSubscriptionAndReturnJson(mockMvc, clientSubscriptionsUri, body);
        return UUID.fromString(new ObjectMapper().readTree(json).get(CLIENT_SUBSCRIPTION_ID).asText());
    }

    public static UUID createSubscriptionCustodialOnly(MockMvc mockMvc, String clientSubscriptionsUri,
                                                       String callbackBaseUrl, String callbackUri) throws Exception {
        String payload = loadPayload(SUBSCRIPTION_CUSTODIAL_ONLY_PATH);
        return createSubscriptionFromPayload(mockMvc, clientSubscriptionsUri, callbackBaseUrl, callbackUri, payload);
    }

    public static UUID createSubscriptionFromPayload(MockMvc mockMvc, String clientSubscriptionsUri,
                                                     String callbackBaseUrl, String callbackUri,
                                                     String payloadWithPlaceholder) throws Exception {
        String callbackUrl = callbackBaseUrl.endsWith("/")
                ? callbackBaseUrl + callbackUri.substring(1)
                : callbackBaseUrl + callbackUri;
        String body = payloadWithPlaceholder.replace(PLACEHOLDER_CALLBACK_URL, callbackUrl);
        String json = postSubscriptionAndReturnJson(mockMvc, clientSubscriptionsUri, body);
        return UUID.fromString(new ObjectMapper().readTree(json).get(CLIENT_SUBSCRIPTION_ID).asText());
    }

    public static String postSubscriptionAndReturnJson(MockMvc mockMvc, String clientSubscriptionsUri,
                                                      String body) throws Exception {
        return mockMvc.perform(post(clientSubscriptionsUri)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientSubscriptionId").exists())
                .andReturn().getResponse().getContentAsString();
    }

    public static ResultActions deleteSubscription(MockMvc mockMvc, String clientSubscriptionsUri,
                                                   UUID clientSubscriptionId) throws Exception {
        return mockMvc.perform(delete(clientSubscriptionsUri + "/{clientSubscriptionId}", clientSubscriptionId));
    }

    private static String loadPayload(String path) throws IOException {
        return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
    }
}
