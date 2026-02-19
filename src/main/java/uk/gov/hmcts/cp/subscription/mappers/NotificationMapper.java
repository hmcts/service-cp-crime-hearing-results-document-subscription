package uk.gov.hmcts.cp.subscription.mappers;

import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import uk.gov.hmcts.cp.openapi.model.EventNotificationPayload;
import uk.gov.hmcts.cp.openapi.model.EventNotificationPayloadCasesInner;
import uk.gov.hmcts.cp.openapi.model.PcrEventPayload;
import uk.gov.hmcts.cp.openapi.model.PcrEventPayloadDefendant;

import java.util.List;
import java.util.UUID;

@Component
public class NotificationMapper {
    private final ObjectMapper objectMapper = new ObjectMapper();

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
        return objectMapper.writeValueAsString(payload);
    }

    public EventNotificationPayload mapFromJson(final String json) {
        return objectMapper.readValue(json, EventNotificationPayload.class);
    }
}
