package uk.gov.hmcts.cp.subscription.model;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class Subscriber {
    private final UUID id;
    private final List<EntityEventType> eventTypes;
    private final String notificationEndpoint;
    private final UUID clientId;
}
