package uk.gov.hmcts.cp.subscription.mappers;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.cp.subscription.entities.ClientSubscriptionEntity;
import uk.gov.hmcts.cp.subscription.model.EntityEventType;
import uk.gov.hmcts.cp.subscription.model.Subscriber;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.cp.subscription.model.EntityEventType.CUSTODIAL_RESULT;
import static uk.gov.hmcts.cp.subscription.model.EntityEventType.PRISON_COURT_REGISTER_GENERATED;

class SubscriberMapperTest {
    SubscriberMapper subscriberMapper = new SubscriberMapper();

    @Test
    void entity_should_map_to_subscriber() {
        UUID id = UUID.randomUUID();
        List<EntityEventType> eventTypes = List.of(PRISON_COURT_REGISTER_GENERATED, CUSTODIAL_RESULT);
        ClientSubscriptionEntity clientSubscription = ClientSubscriptionEntity.builder()
                .id(id)
                .eventTypes(eventTypes)
                .notificationEndpoint("http://localhost")
                .build();

        Subscriber result = subscriberMapper.toSubscriber(clientSubscription);

        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getEventTypes()).isEqualTo(eventTypes);
        assertThat(result.getNotificationEndpoint()).isEqualTo("http://localhost");
        // clientId ??
    }
}