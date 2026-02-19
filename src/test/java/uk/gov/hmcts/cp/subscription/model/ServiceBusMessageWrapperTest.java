package uk.gov.hmcts.cp.subscription.model;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.cp.servicebus.model.ServiceBusMessageWrapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


class ServiceBusMessageWrapperTest {

    UUID correlationId = UUID.fromString("c89dca2c-c8ca-46a7-a139-0cc157180011");

    @Test
    void serialise_object_should_map_to_json_and_back_again() {
        ServiceBusMessageWrapper message = ServiceBusMessageWrapper.builder()
                .correlationId(correlationId)
                .failCount(2)
                .message("{\"key\":\"maybe embedded json\"}")
                .build();

        String messageJson = message.toJson();

        ServiceBusMessageWrapper messageAgain = ServiceBusMessageWrapper.fromJson(messageJson);
        assertThat(messageAgain.getCorrelationId()).isEqualTo(correlationId);
        assertThat(messageAgain.getFailCount()).isEqualTo(2);
        assertThat(messageAgain.getMessage()).isEqualTo("{\"key\":\"maybe embedded json\"}");
    }
}