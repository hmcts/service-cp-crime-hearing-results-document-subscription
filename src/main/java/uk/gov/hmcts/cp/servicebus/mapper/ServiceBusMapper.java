package uk.gov.hmcts.cp.servicebus.mapper;

import com.azure.messaging.servicebus.ServiceBusMessage;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import uk.gov.hmcts.cp.servicebus.model.ServiceBusMessageWrapper;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class ServiceBusMapper {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String mapToJson(final UUID correlationId, final String message, final int failureCount) {
        final ServiceBusMessageWrapper wrapper = ServiceBusMessageWrapper.builder()
                .correlationId(correlationId)
                .message(message)
                .failureCount(failureCount)
                .build();
        return objectMapper.writeValueAsString(wrapper);
    }

    public ServiceBusMessageWrapper mapFromJson(final String json) {
        return objectMapper.readValue(json, ServiceBusMessageWrapper.class);
    }

    public ServiceBusMessage mapToMessage(final String wrappedMessage, final OffsetDateTime nextTryTime) {
        final ServiceBusMessage serviceBusMessage = new ServiceBusMessage(wrappedMessage);
        serviceBusMessage.setScheduledEnqueueTime(nextTryTime);
        return serviceBusMessage;
    }
}
