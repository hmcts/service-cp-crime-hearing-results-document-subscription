package uk.gov.hmcts.cp.notification.mappers;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.cp.openapi.model.EventNotificationPayload;
import uk.gov.hmcts.cp.openapi.model.EventNotificationPayloadCasesInner;
import uk.gov.hmcts.cp.openapi.model.PcrEventPayload;
import uk.gov.hmcts.cp.openapi.model.PcrEventPayloadDefendant;
import uk.gov.hmcts.cp.subscription.services.JsonMapper;

import java.util.List;
import java.util.UUID;

@Component
@AllArgsConstructor
public class NotificationMapper {
    private final JsonMapper jsonMapper;

    public EventNotificationPayload mapToPayload(final UUID documentId, final PcrEventPayload pcrEventPayload) {
        final PcrEventPayloadDefendant defendant = pcrEventPayload.getDefendant();
        final List<EventNotificationPayloadCasesInner> cases = defendant.getCases().stream()
                .map(c -> EventNotificationPayloadCasesInner.builder().urn(c.getUrn()).build())
                .toList();
        final String prisonEmailAddress = defendant.getCustodyEstablishmentDetails().getEmailAddress();
        return EventNotificationPayload.builder()
                .cases(cases)
                .masterDefendantId(defendant.getMasterDefendantId())
                .defendantName(defendant.getName())
                .defendantDateOfBirth(defendant.getDateOfBirth())
                .documentId(documentId)
                .documentGeneratedTimestamp(pcrEventPayload.getTimestamp())
                .prisonEmailAddress(prisonEmailAddress)
                .build();
    }

    public String mapToJson(final EventNotificationPayload payload) {
        return jsonMapper.toJson(payload);
    }

    public EventNotificationPayload mapFromJson(final String json) {
        return jsonMapper.fromJson(json, EventNotificationPayload.class);
    }
}
