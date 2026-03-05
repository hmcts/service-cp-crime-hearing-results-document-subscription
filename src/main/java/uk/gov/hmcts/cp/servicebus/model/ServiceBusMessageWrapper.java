package uk.gov.hmcts.cp.servicebus.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ServiceBusMessageWrapper {
    private int failureCount;
    private String targetUrl;
    private String message;
}
