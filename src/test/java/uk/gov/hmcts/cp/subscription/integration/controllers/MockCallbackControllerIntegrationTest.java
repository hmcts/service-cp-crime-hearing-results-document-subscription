package uk.gov.hmcts.cp.subscription.integration.controllers;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.cp.subscription.integration.IntegrationTestBase;
import uk.gov.hmcts.cp.subscription.services.JsonMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "vault.enabled=false",
        "environment.name=DEV"
})
class MockCallbackControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private JsonMapper jsonMapper;

    private static final String MOCK_CALLBACK_URI = "/mock-callback";
    private static final String PAYLOAD = "{\"eventId\":\"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\"eventType\":\"PRISON_COURT_REGISTER_GENERATED\"}";
    private static final String SUBSCRIPTION_REQUEST = """
            {
              "notificationEndpoint": { "callbackUrl": "https://mock-callback-test.example.com" },
              "eventTypes": ["PRISON_COURT_REGISTER_GENERATED"]
            }
            """;

    private String keyId;
    private String secret;

    @BeforeEach
    void setUp() throws Exception {
        clearAllTables();

        final MvcResult result = mockMvc.perform(post(CLIENT_SUBSCRIPTIONS_URI)
                        .header("Authorization", AUTHORIZATION_HEADER_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SUBSCRIPTION_REQUEST))
                .andExpect(status().isCreated())
                .andReturn();

        final String body = result.getResponse().getContentAsString();
        keyId = jsonMapper.getStringAtPath(body, "/hmac/keyId");
        secret = jsonMapper.getStringAtPath(body, "/hmac/secret");
    }

    @Test
    void valid_signature_should_return_200() throws Exception {
        final String signature = signPayload(secret, PAYLOAD);

        mockMvc.perform(post(MOCK_CALLBACK_URI)
                        .header("X-Key-Id", keyId)
                        .header("X-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD))
                .andExpect(status().isOk());
    }

    @Test
    void invalid_signature_should_return_401() throws Exception {
        mockMvc.perform(post(MOCK_CALLBACK_URI)
                        .header("X-Key-Id", keyId)
                        .header("X-Signature", "invalid-signature==")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD))
                .andExpect(status().isUnauthorized());
    }

    @SneakyThrows
    private String signPayload(final String base64Secret, final String payload) {
        final byte[] secretBytes = Base64.getDecoder().decode(base64Secret);
        final Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secretBytes, "HmacSHA256"));
        final byte[] hmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hmac);
    }

}
