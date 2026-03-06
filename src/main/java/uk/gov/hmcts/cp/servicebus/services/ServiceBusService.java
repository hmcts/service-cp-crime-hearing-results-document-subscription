package uk.gov.hmcts.cp.servicebus.services;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.openapi.model.EventNotificationPayload;
import uk.gov.hmcts.cp.servicebus.config.ServiceBusConfigService;
import uk.gov.hmcts.cp.servicebus.model.ServiceBusMessageWrapper;
import uk.gov.hmcts.cp.subscription.clients.CallbackClient;
import uk.gov.hmcts.cp.subscription.services.JsonMapper;

@Service
@AllArgsConstructor
@Slf4j
public class ServiceBusService {

    private final ServiceBusConfigService configService;
    private final JsonMapper jsonMapper;
    private final ServiceBusRetryService retryService;
    private final CallbackClient callbackClient;

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
        serviceBusMessage.setScheduledEnqueueTime(retryService.getNextTryTime(failureCount));
        serviceBusSenderClient.sendMessage(serviceBusMessage);
        serviceBusSenderClient.close();
        log.info("Queued message to topic:{} with failCount:{}", topicName, failureCount);
    }
}
