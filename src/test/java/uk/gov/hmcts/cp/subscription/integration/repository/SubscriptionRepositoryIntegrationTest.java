package uk.gov.hmcts.cp.subscription.integration.repository;

import static org.assertj.core.api.Assertions.assertThat;

import uk.gov.hmcts.cp.subscription.entities.ClientSubscriptionEntity;
import uk.gov.hmcts.cp.subscription.model.EntityEventType;
import uk.gov.hmcts.cp.subscription.repositories.SubscriptionRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class SubscriptionRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("ampdb")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired(required = false)
    private Flyway flyway;

    @Test
    void repository_should_persist_and_read_against_postgres() {
        // ensure migrations ran (if Flyway is on the classpath and configured)
        if (flyway != null) {
            assertThat(flyway.info().applied()).isNotEmpty();
        }

        ClientSubscriptionEntity entity = ClientSubscriptionEntity.builder()
                .notificationEndpoint("https://example.com/callback")
                .eventTypes(List.of(EntityEventType.PRISON_COURT_REGISTER_GENERATED))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        ClientSubscriptionEntity saved = subscriptionRepository.save(entity);
        assertThat(saved.getId()).isNotNull();

        Optional<ClientSubscriptionEntity> fetched = subscriptionRepository.findById(saved.getId());
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getNotificationEndpoint()).isEqualTo("https://example.com/callback");    }
}
