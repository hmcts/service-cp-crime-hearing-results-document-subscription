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
}