package uk.gov.hmcts.cp.subscription.integration;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
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
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.cp.openapi.model.EventType.CUSTODIAL_RESULT;
import static uk.gov.hmcts.cp.openapi.model.EventType.PRISON_COURT_REGISTER_GENERATED;

@SpringBootTest
@ContextConfiguration(initializers = TestContainersInitialise.class)
@AutoConfigureMockMvc
@Slf4j
public abstract class IntegrationTestBase {

    @Resource
    protected MockMvc mockMvc;

    @Autowired
    protected SubscriptionRepository subscriptionRepository;

    @Autowired
    protected DocumentMappingRepository documentMappingRepository;

    private static final String PCR_REQUEST_TEMPLATE = "stubs/requests/pcr-request.json";

    protected NotificationEndpoint notificationEndpoint = NotificationEndpoint.builder()
            .callbackUrl("https://my-callback-url")
            .build();
    protected ClientSubscriptionRequest request = ClientSubscriptionRequest.builder()
            .notificationEndpoint(notificationEndpoint)
            .eventTypes(List.of(PRISON_COURT_REGISTER_GENERATED, CUSTODIAL_RESULT))
            .build();

    protected void clearClientSubscriptionTable() {
        log.info("Clearing all tables");
        subscriptionRepository.deleteAll();
    }

    protected void clearDocumentMappingTable() {
        log.info("Clearing all tables");
        documentMappingRepository.deleteAll();
    }

    protected ClientSubscriptionEntity insertSubscription(String notificationUri, List<EntityEventType> entityEventTypes) {
        ClientSubscriptionEntity subscription = ClientSubscriptionEntity.builder()
                .eventTypes(entityEventTypes)
                .notificationEndpoint(notificationUri)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        ClientSubscriptionEntity saved = subscriptionRepository.save(subscription);
        log.info("Inserted subscription:{}", saved.getId());
        return saved;
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
        DocumentMappingEntity saved = documentMappingRepository.save(document);
        log.info("Inserted document: {} for materialId:{}, eventType:{}", 
                saved.getDocumentId(), saved.getMaterialId(), saved.getEventType());
        return saved;
    }


    protected String createPcrPayload(UUID eventId, UUID materialId, String eventType) throws IOException {
        ClassPathResource resource = new ClassPathResource(PCR_REQUEST_TEMPLATE);
        String template = Files.readString(resource.getFile().toPath());
        template = template.replaceAll("EVENT_TYPE", eventType);
        if (nonNull(eventId)) {
            template = template.replaceAll("EVENT_ID", eventId.toString());
        } else {
            template = template.replaceAll("EVENT_ID", "null");
        }
        if (nonNull(materialId)) {
            template = template.replaceAll("MATERIAL_ID", materialId.toString());
        } else {
            template = template.replaceAll("MATERIAL_ID", "null");
        }
        return template;
    }
}
