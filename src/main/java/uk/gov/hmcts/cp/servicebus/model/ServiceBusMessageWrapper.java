package uk.gov.hmcts.cp.servicebus.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceBusMessageWrapper {
    private UUID correlationId;
    private int failureCount;
    private String message;
}
