package uk.gov.hmcts.cp.servicebus.services;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.servicebus.config.ServiceBusConfigService;
import uk.gov.hmcts.cp.servicebus.mapper.ServiceBusMapper;

import java.time.OffsetDateTime;

@Service
@AllArgsConstructor
@Slf4j
public class ServiceBusClientService {

    private final ServiceBusConfigService configService;
    private final ServiceBusMapper serviceBusMapper;
    private final ServiceBusRetryService retryService;

    public void queueMessage(final String topicName, final String wrappedMessage, final int failCount) {
        final ServiceBusSenderClient serviceBusSenderClient = configService.senderClient(topicName);
        final OffsetDateTime nextTryTime = retryService.getNextTryTime(failCount);
        final ServiceBusMessage serviceBusMessage = serviceBusMapper.mapToMessage(wrappedMessage, nextTryTime);
        serviceBusSenderClient.sendMessage(serviceBusMessage);
        serviceBusSenderClient.close();
        log.info("Queued message to topic:{} with failCount:{} nextTryTime:{}", topicName, failCount, nextTryTime);
    }
}
