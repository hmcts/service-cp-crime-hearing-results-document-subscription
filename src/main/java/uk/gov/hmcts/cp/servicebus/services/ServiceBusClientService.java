package uk.gov.hmcts.cp.servicebus.services;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.servicebus.config.ServiceBusConfigService;
import uk.gov.hmcts.cp.servicebus.model.ServiceBusMessageWrapper;
import uk.gov.hmcts.cp.subscription.services.JsonMapper;

import java.time.OffsetDateTime;

@Service
@AllArgsConstructor
@Slf4j
public class ServiceBusClientService {

    private final ServiceBusConfigService configService;
    private final JsonMapper jsonMapper;
    private final ServiceBusRetryService retryService;

    public void queueMessage(final String topicName, final String targetUrl, final String messageString, final int failureCount) {
        final ServiceBusSenderClient serviceBusSenderClient = configService
                .clientBuilder()
                .sender()
                .topicName(topicName)
                .buildClient();
        final ServiceBusMessageWrapper messageWrapper = ServiceBusMessageWrapper.builder()
                .failureCount(failureCount)
                .targetUrl(targetUrl)
                .message(messageString)
                .build();
        final ServiceBusMessage serviceBusMessage = new ServiceBusMessage(jsonMapper.toJson(messageWrapper));
        final OffsetDateTime nextTryTime = retryService.getNextTryTime(failureCount);
        serviceBusMessage.setScheduledEnqueueTime(nextTryTime);
        serviceBusSenderClient.sendMessage(serviceBusMessage);
        serviceBusSenderClient.close();
        log.info("Queued message to topic:{} with failCount:{} nextTryTime:{}", topicName, failureCount, nextTryTime);
    }
}
