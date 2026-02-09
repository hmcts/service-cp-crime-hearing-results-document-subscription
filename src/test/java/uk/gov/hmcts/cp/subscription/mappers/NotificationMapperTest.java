package uk.gov.hmcts.cp.subscription.mappers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cp.openapi.model.EventNotificationPayload;
import uk.gov.hmcts.cp.openapi.model.PcrEventPayload;
import uk.gov.hmcts.cp.openapi.model.PcrEventPayloadDefendant;
import uk.gov.hmcts.cp.openapi.model.PcrEventPayloadDefendantCasesInner;
import uk.gov.hmcts.cp.openapi.model.PcrEventPayloadDefendantCustodyEstablishmentDetails;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class NotificationMapperTest {

    @InjectMocks
    NotificationMapper notificationMapper;

    UUID defendentId = UUID.fromString("f791c82b-dd68-468f-9066-a31008f8f229");
    UUID documentId = UUID.fromString("24f36bc0-ecf7-4fa2-985a-afada1cdee98");

    @Test
    void mapper_should_return_populated_object() {
        Instant now = Instant.now();
        PcrEventPayloadDefendantCasesInner caseOne = PcrEventPayloadDefendantCasesInner.builder().urn("http://localhost").build();
        PcrEventPayloadDefendant.builder().cases(List.of(caseOne)).build();
        PcrEventPayloadDefendantCustodyEstablishmentDetails custodyEstablishment = PcrEventPayloadDefendantCustodyEstablishmentDetails.builder()
                .emailAddress("prison@example.com")
                .build();
        PcrEventPayloadDefendant defendant = PcrEventPayloadDefendant.builder()
                .cases(List.of(caseOne))
                .masterDefendantId(defendentId)
                .name("John Doe")
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .custodyEstablishmentDetails(custodyEstablishment)
                .build();
        PcrEventPayload pcrEventPayload = PcrEventPayload.builder()
                .defendant(defendant)
                .timestamp(now)
                .build();

        EventNotificationPayload response = notificationMapper.mapToPayload(documentId, pcrEventPayload);

        assertThat(response.getCases()).hasSize(1);
        assertThat(response.getCases().get(0).getUrn()).isEqualTo("http://localhost");
        assertThat(response.getMasterDefendantId()).isEqualTo(defendentId);
        assertThat(response.getDefendantName()).isEqualTo("John Doe");
        assertThat(response.getDefendantDateOfBirth()).isEqualTo(LocalDate.of(2000, 1, 1));
        assertThat(response.getDocumentId()).isEqualTo(documentId);
        assertThat(response.getDocumentGeneratedTimestamp()).isEqualTo(now);
        assertThat(response.getPrisonEmailAddress()).isEqualTo("prison@example.com");
    }
}