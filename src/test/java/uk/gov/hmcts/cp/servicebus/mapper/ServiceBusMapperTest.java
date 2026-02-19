package uk.gov.hmcts.cp.servicebus.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cp.servicebus.model.ServiceBusMessageWrapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ServiceBusMapperTest {

    @InjectMocks
    ServiceBusMapper serviceBusMapper;

    UUID correlationId = UUID.fromString("45b9f842-c8c8-4fa0-86d0-707858f283f3");

    @Test
    void serialise_message_should_map_to_json_and_back_again() {

        String messageJson = serviceBusMapper.mapToJson(correlationId, "{\"key\":\"maybe embedded json\"}", 2);

        ServiceBusMessageWrapper messageAgain = serviceBusMapper.mapFromJson(messageJson);
        assertThat(messageAgain.getCorrelationId()).isEqualTo(correlationId);
        assertThat(messageAgain.getFailureCount()).isEqualTo(2);
        assertThat(messageAgain.getMessage()).isEqualTo("{\"key\":\"maybe embedded json\"}");
    }
}