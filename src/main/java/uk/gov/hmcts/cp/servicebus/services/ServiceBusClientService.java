package uk.gov.hmcts.cp.servicebus.services;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.servicebus.config.ServiceBusConfigService;
import uk.gov.hmcts.cp.servicebus.mapper.ServiceBusMapper;
import uk.gov.hmcts.cp.servicebus.model.ServiceBusMessageWrapper;
import uk.gov.hmcts.cp.subscription.services.JsonMapper;

import java.time.OffsetDateTime;

@Service
@AllArgsConstructor
@Slf4j
public class ServiceBusClientService {

    private final ServiceBusConfigService configService;
    private final ServiceBusMapper serviceBusMapper;
    private final JsonMapper jsonMapper;
    private final ServiceBusRetryService retryService;

    public void queueMessage(final String topicName, final String messageString, final int failCount) {
        final ServiceBusSenderClient serviceBusSenderClient = configService.senderClient(topicName);
        // TODO make this a mapper method
        final ServiceBusMessageWrapper wrapper = ServiceBusMessageWrapper.builder()
                .failureCount(0)
                .targetUrl("url-todo")
                .message(messageString)
                .build();
        final String wrappedMessage = jsonMapper.toJson(wrapper);
        final OffsetDateTime nextTryTime = retryService.getNextTryTime(failCount);
        final ServiceBusMessage serviceBusMessage = serviceBusMapper.mapToMessage(wrappedMessage, nextTryTime);
        serviceBusSenderClient.sendMessage(serviceBusMessage);
        serviceBusSenderClient.close();
        log.info("Queued message to topic:{} with failCount:{}", topicName, failCount);
    }
}
