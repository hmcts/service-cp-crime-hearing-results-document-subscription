package uk.gov.hmcts.cp.subscription.mappers;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.cp.subscription.entities.ClientSubscriptionEntity;
import uk.gov.hmcts.cp.subscription.model.Subscriber;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class SubscriberMapper {

    public Subscriber toSubscriber(final ClientSubscriptionEntity entity) {
        final List<String> eventTypeNames = entity.getEventTypes().stream()
                .map(Enum::name)
                .collect(Collectors.toList());
        return new Subscriber(
                entity.getId(),
                entity.getEventTypes(),
                entity.getNotificationEndpoint(),
                null
        );
    }
}
