package uk.gov.hmcts.cp.servicebus.services;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.servicebus.mapper.ServiceBusMapper;
import uk.gov.hmcts.cp.subscription.config.ServiceBusConfigService;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class ServiceBusClientService {

    private final ServiceBusConfigService configService;
    private final ServiceBusMapper serviceBusMapper;
    private final ServiceBusRetryService retryService;

    public void queueMessage(final String topicName, final UUID correlationId, final String messageString, final int failCount) {
        final ServiceBusSenderClient serviceBusSenderClient = configService.senderClient(topicName);
        final String wrappedMessage = serviceBusMapper.mapToJson(correlationId, messageString, failCount);
        final OffsetDateTime nextTryTime = retryService.getNextTryTime(failCount);
        final ServiceBusMessage serviceBusMessage = serviceBusMapper.mapToMessage(wrappedMessage, nextTryTime);
        serviceBusSenderClient.sendMessage(serviceBusMessage);
        serviceBusSenderClient.close();
        log.info("Queued message to topic:{} with failCount:{} and correlationId:{}", topicName, failCount, correlationId);
    }
}
