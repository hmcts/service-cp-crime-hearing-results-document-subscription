package uk.gov.hmcts.cp.servicebus.mapper;

import com.azure.messaging.servicebus.ServiceBusMessage;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.cp.servicebus.model.ServiceBusMessageWrapper;
import uk.gov.hmcts.cp.subscription.services.JsonMapper;

import java.time.OffsetDateTime;

@Component
@AllArgsConstructor
public class ServiceBusMapper {
    private final JsonMapper jsonMapper;

    // Drop this in favour of JsonMapper
    public ServiceBusMessageWrapper mapFromJson(final String json) {
        return jsonMapper.fromJson(json, ServiceBusMessageWrapper.class);
    }

    public ServiceBusMessage mapToMessage(final String wrappedMessage, final OffsetDateTime nextTryTime) {
        final ServiceBusMessage serviceBusMessage = new ServiceBusMessage(wrappedMessage);
        serviceBusMessage.setScheduledEnqueueTime(nextTryTime);
        return serviceBusMessage;
    }
}
