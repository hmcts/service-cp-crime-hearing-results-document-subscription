package uk.gov.hmcts.cp.servicebus.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cp.servicebus.model.ServiceBusMessageWrapper;
import uk.gov.hmcts.cp.subscription.services.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ServiceBusMapperTest {

    @Spy
    JsonMapper jsonMapper = new JsonMapper();

    @InjectMocks
    ServiceBusMapper serviceBusMapper;

    @Test
    void serialise_message_should_map_to_json_and_back_again() {

        String messageJson = serviceBusMapper.mapToJson("{\"key\":\"maybe embedded json\"}", 2);

        ServiceBusMessageWrapper messageAgain = serviceBusMapper.mapFromJson(messageJson);
        assertThat(messageAgain.getFailureCount()).isEqualTo(2);
        assertThat(messageAgain.getMessage()).isEqualTo("{\"key\":\"maybe embedded json\"}");
    }
}