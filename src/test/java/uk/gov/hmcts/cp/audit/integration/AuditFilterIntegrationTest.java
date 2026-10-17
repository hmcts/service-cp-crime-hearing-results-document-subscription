package uk.gov.hmcts.cp.audit.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import uk.gov.hmcts.cp.audit.config.ArtemisAuditAutoConfiguration;
import uk.gov.hmcts.cp.audit.model.AuditMessage;
import uk.gov.hmcts.cp.audit.service.AuditClockService;
import uk.gov.hmcts.cp.audit.service.AuditSenderService;
import uk.gov.hmcts.cp.audit.service.AuditUuidService;
import uk.gov.hmcts.cp.subscription.integration.IntegrationTestBase;
import uk.gov.hmcts.cp.subscription.integration.stubs.MaterialStub;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EnableWireMock({@ConfigureWireMock(name = "material-client", baseUrlProperties = "material-client.url", port = 0, filesUnderClasspath = "wiremock/material-client")})
@TestPropertySource(properties = "cp.audit.enabled=true")
class AuditFilterIntegrationTest extends IntegrationTestBase {

    private static final Instant FIXED_TIME          = Instant.parse("2026-01-01T10:00:00Z");
    private static final String  CJSCPPUID_HEADER     = "CJSCPPUID";
    private static final String  TEST_USER_ID         = "99999999-8888-7777-6666-555555555555";
    private static final UUID    FIXED_METADATA_ID    = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final UUID    FIXED_CORRELATION_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID    FIXED_SUBSCRIPTION_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID    FIXED_DOCUMENT_ID    = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    private static final UUID    FIXED_MATERIAL_ID    = UUID.fromString("04325082-5203-4eaa-9f62-e153d6308631");

    private static final ObjectMapper MAPPER = new ArtemisAuditAutoConfiguration().auditObjectMapper();

    @MockitoBean
    AuditSenderService auditSenderService;

    @MockitoBean(name = "auditClockService")
    AuditClockService auditClockService;

    @MockitoBean
    AuditUuidService auditUuidService;

    ArgumentCaptor<AuditMessage> payloadCaptor;

    @BeforeEach
    void setUp() {
        clearAllTables();
        when(auditClockService.now()).thenReturn(FIXED_TIME);
        when(auditUuidService.randomUUID()).thenReturn(FIXED_METADATA_ID);
        payloadCaptor = ArgumentCaptor.forClass(AuditMessage.class);
    }

    @Test
    void creating_notification_should_not_send_audit_event_because_endpoint_is_excluded() throws Exception {
        mockMvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventType\":\"UNKNOWN_EVENT_TYPE\",\"eventId\":\"a4554152-10fb-44fe-a015-226f8d547c91\","
                                + "\"materialId\":\"886a3d9c-2543-4fdd-8b5c-1597e3d36ebb\","
                                + "\"hearingId\":\"b2c3d4e5-f6a7-8901-bcde-f12345678901\","
                                + "\"timestamp\":\"2026-05-29T10:23:29Z\","
                                + "\"defendant\":{\"masterDefendantId\":\"f08465c5-0000-0000-0000-000000000000\","
                                + "\"name\":\"Test Defendant\",\"dateOfBirth\":\"2000-01-01\",\"cases\":[{\"urn\":\"TEST123\"}]}}"))
                .andExpect(status().isAccepted());

        verify(auditSenderService, never()).send(any());
    }

    @Test
    void getting_client_subscription_should_send_request_and_response_audit_events() throws Exception {
        insertSubscription(FIXED_SUBSCRIPTION_ID, TEST_CLIENT_ID, List.of("PRISON_COURT_REGISTER_GENERATED"),
                "https://callback", "kid-v1-keyid");

        mockMvc.perform(get("/client-subscriptions/{subscriptionId}", FIXED_SUBSCRIPTION_ID)
                        .header(AUTHORIZATION, AUTHORIZATION_HEADER_VALUE)
                        .header(CJSCPPUID_HEADER, TEST_USER_ID)
                        .header("X-Correlation-Id", FIXED_CORRELATION_ID))
                .andExpect(status().isOk());

        assertAuditPayloads("get-client-subscription");
    }

    @Test
    void getting_document_should_send_request_and_response_audit_events_with_material_id() throws Exception {
        insertSubscription(FIXED_SUBSCRIPTION_ID, TEST_CLIENT_ID, List.of("PRISON_COURT_REGISTER_GENERATED"),
                "https://callback", "kid-v1-keyid");
        insertDocument(FIXED_DOCUMENT_ID, FIXED_MATERIAL_ID, "PRISON_COURT_REGISTER_GENERATED");
        MaterialStub.stubMaterialMetadata(FIXED_MATERIAL_ID);
        MaterialStub.stubMaterialContent(FIXED_MATERIAL_ID);
        MaterialStub.stubMaterialBinary(FIXED_MATERIAL_ID);

        mockMvc.perform(get("/client-subscriptions/{clientSubscriptionId}/documents/{documentId}",
                        FIXED_SUBSCRIPTION_ID, FIXED_DOCUMENT_ID)
                        .header(AUTHORIZATION, AUTHORIZATION_HEADER_VALUE)
                        .header("X-Correlation-Id", FIXED_CORRELATION_ID))
                .andExpect(status().isOk());

        assertAuditPayloads("get-document");
    }

    @Test
    void creating_client_subscription_should_send_request_and_response_audit_events() throws Exception {
        mockMvc.perform(post("/client-subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loadPayload("stubs/requests/subscription/subscription-request-valid.json"))
                        .header(AUTHORIZATION, AUTHORIZATION_HEADER_VALUE)
                        .header(CJSCPPUID_HEADER, TEST_USER_ID)
                        .header("X-Correlation-Id", FIXED_CORRELATION_ID))
                .andExpect(status().isCreated());

        assertAuditPayloads("create-client-subscription");
    }

    @Test
    void updating_client_subscription_should_send_request_and_response_audit_events() throws Exception {
        insertSubscription(FIXED_SUBSCRIPTION_ID, TEST_CLIENT_ID, List.of("PRISON_COURT_REGISTER_GENERATED"),
                "https://callback", "kid-v1-keyid");

        mockMvc.perform(put("/client-subscriptions/{id}", FIXED_SUBSCRIPTION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loadPayload("stubs/requests/subscription/subscription-request-valid.json"))
                        .header(AUTHORIZATION, AUTHORIZATION_HEADER_VALUE)
                        .header(CJSCPPUID_HEADER, TEST_USER_ID)
                        .header("X-Correlation-Id", FIXED_CORRELATION_ID))
                .andExpect(status().isOk());

        assertAuditPayloads("update-client-subscription");
    }

    @Test
    void deleting_client_subscription_should_send_request_and_response_audit_events() throws Exception {
        insertSubscription(FIXED_SUBSCRIPTION_ID, TEST_CLIENT_ID, List.of("PRISON_COURT_REGISTER_GENERATED"),
                "https://callback", "kid-v1-keyid");

        mockMvc.perform(delete("/client-subscriptions/{id}", FIXED_SUBSCRIPTION_ID)
                        .header(AUTHORIZATION, AUTHORIZATION_HEADER_VALUE)
                        .header(CJSCPPUID_HEADER, TEST_USER_ID)
                        .header("X-Correlation-Id", FIXED_CORRELATION_ID))
                .andExpect(status().isNoContent());

        assertAuditPayloads("delete-client-subscription");
    }

    @Test
    void getting_client_subscription_when_audit_sender_fails_should_still_return_200() throws Exception {
        insertSubscription(FIXED_SUBSCRIPTION_ID, TEST_CLIENT_ID, List.of("PRISON_COURT_REGISTER_GENERATED"),
                "https://callback", "kid-v1-keyid");
        doThrow(new RuntimeException("audit broker down")).when(auditSenderService).send(any());

        mockMvc.perform(get("/client-subscriptions/{subscriptionId}", FIXED_SUBSCRIPTION_ID)
                        .header(AUTHORIZATION, AUTHORIZATION_HEADER_VALUE)
                        .header("X-Correlation-Id", FIXED_CORRELATION_ID))
                .andExpect(status().isOk());
    }

    private void assertAuditPayloads(final String endpoint) throws Exception {
        verify(auditSenderService, times(2)).send(payloadCaptor.capture());
        final List<AuditMessage> payloads = payloadCaptor.getAllValues();
        JSONAssert.assertEquals(
                loadPayload("audit/" + endpoint + "-request.json"),
                MAPPER.writeValueAsString(payloads.get(0)),
                JSONCompareMode.STRICT);
        JSONAssert.assertEquals(
                loadPayload("audit/" + endpoint + "-response.json"),
                MAPPER.writeValueAsString(payloads.get(1)),
                JSONCompareMode.STRICT);
    }
}
