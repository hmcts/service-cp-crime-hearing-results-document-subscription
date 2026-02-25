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
import uk.gov.hmcts.cp.subscription.integration.helpers.JwtHelper;
import uk.gov.hmcts.cp.subscription.model.EntityEventType;
import uk.gov.hmcts.cp.subscription.repositories.DocumentMappingRepository;
import uk.gov.hmcts.cp.subscription.repositories.SubscriptionRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static uk.gov.hmcts.cp.openapi.model.EventType.CUSTODIAL_RESULT;
import static uk.gov.hmcts.cp.openapi.model.EventType.PRISON_COURT_REGISTER_GENERATED;

@SpringBootTest
@ContextConfiguration(initializers = TestContainersInitialise.class)
@AutoConfigureMockMvc
@Slf4j
public abstract class IntegrationTestBase {

    protected static final UUID MATERIAL_ID_TIMEOUT = UUID.fromString("11111111-1111-1111-1111-111111111112");
    protected static final String NOTIFICATIONS_PCR_URI = "/notifications/pcr";
    protected static final String CLIENT_SUBSCRIPTIONS_URI = "/client-subscriptions";
    protected static final String CALLBACK_URI = "/callback/notify";
    protected static final UUID TEST_CLIENT_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    protected static final String AUTHORIZATION_HEADER_VALUE = JwtHelper.bearerTokenWithAzp(TEST_CLIENT_ID.toString());

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
        return insertSubscription(notificationUri, entityEventTypes, TEST_CLIENT_ID);
    }

    protected ClientSubscriptionEntity insertSubscription(String notificationUri, List<EntityEventType> entityEventTypes, UUID clientId) {
        ClientSubscriptionEntity subscription = ClientSubscriptionEntity.builder()
                .clientId(clientId)
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
}
