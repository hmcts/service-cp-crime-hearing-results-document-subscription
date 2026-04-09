package uk.gov.hmcts.cp.subscription.integration.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import uk.gov.hmcts.cp.openapi.model.EventPayload;
import uk.gov.hmcts.cp.servicebus.services.ServiceBusClientService;
import uk.gov.hmcts.cp.servicebus.services.ServiceBusHandlers;
import uk.gov.hmcts.cp.subscription.entities.DocumentMappingEntity;
import uk.gov.hmcts.cp.subscription.integration.IntegrationTestBase;
import uk.gov.hmcts.cp.subscription.services.CallbackDeliveryService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EnableWireMock({@ConfigureWireMock(name = "material-client", baseUrlProperties = "material-client.url", port = 0, filesUnderClasspath = "wiremock/material-client")})
@TestPropertySource(properties = {
        "material-service.url=the-real-dev-one",
        "material-client.cjscppuid=the-real-one"
})
class NotificationControllerIntegrationTest extends IntegrationTestBase {

    private static final String NOTIFICATION_URI = "/notifications";
    private static final String CALLBACK_URL = "https://callback.example.com";
    private static final UUID MATERIAL_ID = UUID.fromString("04325082-5203-4eaa-9f62-e153d6308631");
    private static final String DOCUMENT_URI = "/client-subscriptions/{clientSubscriptionId}/documents/{documentId}";

    @MockitoBean
    private CallbackDeliveryService callbackDeliveryService;

    @MockitoBean
    private ServiceBusClientService serviceBusClientService;

    @Autowired
    private ServiceBusHandlers serviceBusHandlers;

    @BeforeEach
    void setUp() {
        reset(callbackDeliveryService);
        doAnswer(invocation -> {
            serviceBusHandlers.handleMessage(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2));
            return null;
        }).when(serviceBusClientService).queueMessage(anyString(), any(), anyString(), anyInt());
        clearAllTables();
    }

    @Test
    void prison_court_register_generated_should_return_success() throws Exception {
        String payload = loadPayload("stubs/requests/progression/pcr-request-prison-court-register.json");

        mockMvc.perform(post(NOTIFICATION_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                        .content(payload))
                .andExpect(status().isAccepted());

        verify(callbackDeliveryService, times(1)).submitOutboundEvents(any(EventPayload.class), any(UUID.class));
    }

    // Reproduces the Azure Blob 403 "Signature fields not well formed" bug.
    // Without URI.create(): RestTemplate re-encodes %2B -> %252B in the SAS query params,
    // breaking the signature. WireMock simulates Azure's behaviour:
    //   - stub - material-blob-large-file-mapping.json — exact URL with %2B preserved → returns 200
    //   - stub - material-blob-auth-fail-mapping.json — any blob path → returns 403
    @Test
    void get_document_should_return_200_with_pdf_when_subscription_has_access() throws Exception {
        UUID subscriptionId = insertSubscription(
                CALLBACK_URL,
                List.of("PRISON_COURT_REGISTER_GENERATED"));
        DocumentMappingEntity document = insertDocument(MATERIAL_ID, "PRISON_COURT_REGISTER_GENERATED");

        mockMvc.perform(get(DOCUMENT_URI,
                        subscriptionId, document.getDocumentId())
                        .header("Authorization", AUTHORIZATION_HEADER_VALUE))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"PrisonCourtRegister_20260325161340.pdf\""))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

}
