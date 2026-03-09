package uk.gov.hmcts.cp.servicebus.mapper;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.cp.servicebus.model.ServiceBusWrappedMessage;

@Component
@AllArgsConstructor
public class ServiceBusWrapperMapper {

    public ServiceBusWrappedMessage newWrapper(final int failureCount, final String targetUrl, final String message) {
        return ServiceBusWrappedMessage.builder()
                .failureCount(failureCount)
                .targetUrl(targetUrl)
                .message(message)
                .build();
    }
}
