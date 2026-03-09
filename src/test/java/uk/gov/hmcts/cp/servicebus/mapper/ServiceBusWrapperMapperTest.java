package uk.gov.hmcts.cp.servicebus.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cp.servicebus.model.ServiceBusWrappedMessage;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ServiceBusWrapperMapperTest {

    @InjectMocks
    ServiceBusWrapperMapper wrapperMapper;

    @Test
    void mapper_should_create_new_object() {
        ServiceBusWrappedMessage response = wrapperMapper.newWrapper(1, "https://callback", "wrapped-message");
        assertThat(response.getFailureCount()).isEqualTo(1);
        assertThat(response.getTargetUrl()).isEqualTo("https://callback");
        assertThat(response.getMessage()).isEqualTo("wrapped-message");
    }
}