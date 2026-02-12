package uk.gov.hmcts.cp.subscription.integration;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import uk.gov.hmcts.cp.openapi.model.ClientSubscriptionRequest;
import uk.gov.hmcts.cp.openapi.model.NotificationEndpoint;
import uk.gov.hmcts.cp.subscription.integration.config.TestContainersInitialise;
import uk.gov.hmcts.cp.subscription.entities.ClientSubscriptionEntity;
import uk.gov.hmcts.cp.subscription.entities.DocumentMappingEntity;
import uk.gov.hmcts.cp.subscription.model.EntityEventType;
import uk.gov.hmcts.cp.subscription.repositories.DocumentMappingRepository;
import uk.gov.hmcts.cp.subscription.repositories.SubscriptionRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.github.tomakehurst.wiremock.WireMockServer;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static uk.gov.hmcts.cp.openapi.model.EventType.CUSTODIAL_RESULT;
import static uk.gov.hmcts.cp.openapi.model.EventType.PRISON_COURT_REGISTER_GENERATED;

@SpringBootTest
@ContextConfiguration(initializers = TestContainersInitialise.class)
@AutoConfigureMockMvc
@Slf4j
public abstract class IntegrationTestBase {

    private static final String MATERIAL_METADATA_RESPONSE_PATH = "wiremock/material-client/files/material-response.json";
    private static final String MATERIAL_CONTENT_RESPONSE_PATH = "wiremock/material-client/files/material-with-contenturl.json";
    private static final String MATERIAL_PDF_PATH = "wiremock/material-client/files/material-content.pdf";
    private static final String CALLBACK_RESPONSE_PATH = "wiremock/callback-client/files/callback-accepted.json";
    private static final String MATERIAL_URI = "/material-query-api/query/api/rest/material/material/";
    private static final String METADATA = "/metadata";
    private static final String APPLICATION_JSON = "application/json";
    private static final String CONTENT_TYPE = "Content-Type";

    protected static final UUID MATERIAL_ID_TIMEOUT = UUID.fromString("11111111-1111-1111-1111-111111111112");
    protected static final String NOTIFICATIONS_PCR_URI = "/notifications/pcr";
    protected static final String CLIENT_SUBSCRIPTIONS_URI = "/client-subscriptions";
    protected static final String CALLBACK_URI = "/callback/notify";

    @Resource
    protected MockMvc mockMvc;

    @Autowired
    protected SubscriptionRepository subscriptionRepository;

    @Autowired
    protected DocumentMappingRepository documentMappingRepository;

    protected NotificationEndpoint notificationEndpoint = NotificationEndpoint.builder()
            .callbackUrl("https://my-callback-url")
            .build();
    protected ClientSubscriptionRequest request = ClientSubscriptionRequest.builder()
            .notificationEndpoint(notificationEndpoint)
            .eventTypes(List.of(PRISON_COURT_REGISTER_GENERATED, CUSTODIAL_RESULT))
            .build();

    protected void clearClientSubscriptionTable() {
        log.info("Clearing client_subscription table");
        subscriptionRepository.deleteAll();
    }

    protected void clearDocumentMappingTable() {
        log.info("Clearing document_mapping table");
        documentMappingRepository.deleteAll();
    }

    protected void clearAllTables() {
        log.info("Clearing client_subscription and document_mapping tables");
        subscriptionRepository.deleteAll();
        documentMappingRepository.deleteAll();
    }

    protected ClientSubscriptionEntity insertSubscription(String notificationUri, List<EntityEventType> entityEventTypes) {
        ClientSubscriptionEntity subscription = ClientSubscriptionEntity.builder()
                .eventTypes(entityEventTypes)
                .notificationEndpoint(notificationUri)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        return subscriptionRepository.save(subscription);
    }

    protected DocumentMappingEntity insertDocument(UUID materialId) {
        return insertDocument(materialId, EntityEventType.PRISON_COURT_REGISTER_GENERATED);
    }

    protected DocumentMappingEntity insertDocument(UUID materialId, EntityEventType eventType) {
        DocumentMappingEntity document = DocumentMappingEntity.builder()
                .materialId(materialId)
                .eventType(eventType)
                .createdAt(OffsetDateTime.now())
                .build();
        return documentMappingRepository.save(document);
    }

    protected String loadPayload(String path) throws IOException {
        return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
    }

    protected void stubMaterialMetadata(UUID materialId) throws IOException {
        String materialPath = MATERIAL_URI + materialId;
        String metadataBody = new ClassPathResource(MATERIAL_METADATA_RESPONSE_PATH).getContentAsString(StandardCharsets.UTF_8);
        stubFor(get(urlPathMatching(".*" + materialPath + METADATA))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(metadataBody)));
    }

    protected void stubMaterialContent(UUID materialId) throws IOException {
        String materialPath = MATERIAL_URI + materialId;
        String contentBody = new ClassPathResource(MATERIAL_CONTENT_RESPONSE_PATH).getContentAsString(StandardCharsets.UTF_8);
        stubFor(get(urlPathMatching(".*" + materialPath + "/content"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, "application/vnd.material.query.material+json")
                        .withBody(contentBody)
                        .withTransformers("response-template")));
    }

    protected void stubMaterialBinary(UUID materialId) throws IOException {
        String materialPath = MATERIAL_URI + materialId;
        byte[] pdfBody = new ClassPathResource(MATERIAL_PDF_PATH).getContentAsByteArray();
        stubFor(get(urlPathMatching(".*" + materialPath + "/binary"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, "application/pdf")
                        .withHeader("Content-Disposition", "inline; filename=\"material-content.pdf\"")
                        .withBody(pdfBody)));
    }

    protected void stubMaterialMetadataNoContent(UUID materialId) {
        String materialPath = MATERIAL_URI + materialId;
        stubFor(get(urlPathMatching(".*" + materialPath + METADATA))
                .willReturn(aResponse().withStatus(204)));
    }

    protected void stubCallbackEndpoint(WireMockServer server, String callbackUri) throws IOException {
        String body = new ClassPathResource(CALLBACK_RESPONSE_PATH).getContentAsString(StandardCharsets.UTF_8);
        server.stubFor(post(urlPathEqualTo(callbackUri))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(body)));
    }
}
