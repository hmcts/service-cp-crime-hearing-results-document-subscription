package uk.gov.hmcts.cp.servicebus.mapper;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.cp.servicebus.model.ServiceBusMessageWrapper;

@Component
@AllArgsConstructor
public class ServiceBusWrapperMapper {

    public ServiceBusMessageWrapper newWrapper(final String message, final String targetUrl) {
        return ServiceBusMessageWrapper.builder()
                .failureCount(0)
                .targetUrl(targetUrl)
                .message(message)
                .build();
    }

    public ServiceBusMessageWrapper incrementFailureCount(final ServiceBusMessageWrapper wrapper) {
        final int newFailureCount = wrapper.getFailureCount() + 1;
        return wrapper.toBuilder()
                .failureCount(newFailureCount)
                .build();
    }
}
