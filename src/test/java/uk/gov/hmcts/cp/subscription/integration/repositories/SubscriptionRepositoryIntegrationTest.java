package uk.gov.hmcts.cp.subscription.integration.repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.cp.subscription.entities.ClientSubscriptionEntity;
import uk.gov.hmcts.cp.subscription.integration.IntegrationTestBase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.cp.subscription.model.EntityEventType.CUSTODIAL_RESULT;
import static uk.gov.hmcts.cp.subscription.model.EntityEventType.PRISON_COURT_REGISTER_GENERATED;

class SubscriptionRepositoryIntegrationTest extends IntegrationTestBase {

    @BeforeEach
    void beforeEach() {
        clearClientSubscriptionTable();
    }

    @Transactional
    @Test
    void subscription_should_save_and_read_ok() {
        ClientSubscriptionEntity saved = insertSubscription("https://example.com/notify", List.of(CUSTODIAL_RESULT, PRISON_COURT_REGISTER_GENERATED));
        ClientSubscriptionEntity found = subscriptionRepository.getReferenceById(saved.getId());
        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getEventTypes()).isEqualTo(List.of(CUSTODIAL_RESULT, PRISON_COURT_REGISTER_GENERATED));
        assertThat(found.getNotificationEndpoint()).isEqualTo("https://example.com/notify");
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Transactional
    @Test
    void subscription_should_delete_ok() {
        ClientSubscriptionEntity saved = insertSubscription("https://example.com/notify", List.of(CUSTODIAL_RESULT, PRISON_COURT_REGISTER_GENERATED));
        subscriptionRepository.deleteById(saved.getId());
        assertThat(subscriptionRepository.findAll()).hasSize(0);
    }

    @Transactional
    @Test
    void findByEventType_should_return_subscriptions_for_event_type() {
        insertSubscription("https://subscriber1.example/callback", List.of(PRISON_COURT_REGISTER_GENERATED));
        insertSubscription("https://subscriber2.example/callback", List.of(CUSTODIAL_RESULT));
        insertSubscription("https://subscriber3.example/callback", List.of(PRISON_COURT_REGISTER_GENERATED, CUSTODIAL_RESULT));

        var forPcr = subscriptionRepository.findByEventType("PRISON_COURT_REGISTER_GENERATED");
        var forCustodial = subscriptionRepository.findByEventType("CUSTODIAL_RESULT");

        assertThat(forPcr).hasSize(2);
        assertThat(forCustodial).hasSize(2);
    }
}