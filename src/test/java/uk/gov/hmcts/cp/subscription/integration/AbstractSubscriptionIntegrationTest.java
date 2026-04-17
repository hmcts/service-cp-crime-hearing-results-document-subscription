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
import uk.gov.hmcts.cp.subscription.entities.ClientEntity;
import uk.gov.hmcts.cp.subscription.entities.ClientEventEntity;
import uk.gov.hmcts.cp.subscription.entities.ClientHmacEntity;
import uk.gov.hmcts.cp.subscription.entities.DocumentMappingEntity;
import uk.gov.hmcts.cp.subscription.integration.config.TestContainersInitialise;
import uk.gov.hmcts.cp.subscription.integration.helpers.JwtHelper;
import uk.gov.hmcts.cp.subscription.repositories.ClientEventRepository;
import uk.gov.hmcts.cp.subscription.repositories.ClientHmacRepository;
import uk.gov.hmcts.cp.subscription.repositories.ClientRepository;
import uk.gov.hmcts.cp.subscription.repositories.DocumentMappingRepository;
import uk.gov.hmcts.cp.subscription.repositories.EventTypeRepository;
import uk.gov.hmcts.cp.subscription.services.ClockService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Shared Spring + DB setup for subscription integration tests. Subclasses that need a live
 * Service Bus processor (e.g. async E2E) extend this directly; most tests use {@link IntegrationTestBase}.
 */
@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = TestContainersInitialise.class)
@TestPropertySource(properties = {
        "vault.enabled=false"
})
public abstract class AbstractSubscriptionIntegrationTest {

    protected static final String NOTIFICATIONS_URI = "/notifications";
    protected static final String CLIENT_SUBSCRIPTIONS_URI = "/client-subscriptions";
    protected static final String CALLBACK_URI = "/callback/notify";
    protected static final UUID TEST_CLIENT_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    protected static final String AUTHORIZATION_HEADER_VALUE = JwtHelper.bearerTokenWithAzp(TEST_CLIENT_ID.toString());

    @Resource
    protected MockMvc mockMvc;

    @Autowired
    protected ClientHmacRepository clientHmacRepository;

    @Autowired
    protected DocumentMappingRepository documentMappingRepository;

    @Autowired
    protected EventTypeRepository eventTypeRepository;

    @Autowired
    protected ClientRepository clientRepository;

    @Autowired
    protected ClientEventRepository clientEventRepository;

    @Autowired
    protected ClockService clockService;

    protected NotificationEndpoint notificationEndpoint = NotificationEndpoint.builder()
            .callbackUrl("https://my-callback-url")
            .build();
    protected ClientSubscriptionRequest request = ClientSubscriptionRequest.builder()
            .notificationEndpoint(notificationEndpoint)
            .eventTypes(List.of("PRISON_COURT_REGISTER_GENERATED"))
            .build();

    protected void clearAllTables() {
        log.info("Clearing all tables");
        clientHmacRepository.deleteAll();
        clientEventRepository.deleteAll();
        clientRepository.deleteAll();
        documentMappingRepository.deleteAll();
    }


    protected String loadPayload(String path) throws IOException {
        return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
    }
}
