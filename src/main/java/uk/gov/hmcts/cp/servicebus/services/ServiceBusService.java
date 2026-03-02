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
import uk.gov.hmcts.cp.servicebus.model.ServiceBusMessageWrapper;
import uk.gov.hmcts.cp.subscription.clients.CallbackClient;
import uk.gov.hmcts.cp.servicebus.config.ServiceBusConfigService;
import uk.gov.hmcts.cp.subscription.mappers.NotificationMapper;
import uk.gov.hmcts.cp.subscription.services.JsonMapper;

@Service
@AllArgsConstructor
@Slf4j
public class ServiceBusService {

    private final ServiceBusConfigService configService;
    private final JsonMapper jsonMapper;
    private final NotificationMapper notificationMapper;
    private final ServiceBusRetryService retryService;
    private final CallbackClient callbackClient;

    public void queueMessage(String topicName, String messageString, int failCount) {
        ServiceBusMessageWrapper message = ServiceBusMessageWrapper.builder()
                .failureCount(failCount)
                .message(messageString)
                .build();
        ServiceBusSenderClient serviceBusSenderClient = configService
                .clientBuilder()
                .sender()
                .topicName(topicName)
                .buildClient();
        ServiceBusMessage serviceBusMessage = jsonMapper.fromJson(messageString, ServiceBusMessage.class);
        serviceBusMessage.setScheduledEnqueueTime(retryService.getNextTryTime(failCount));
        serviceBusSenderClient.sendMessage(serviceBusMessage);
        serviceBusSenderClient.close();
        log.info("Queued message to topic:{} with failCount:{}", topicName, failCount);
    }

    @SneakyThrows
    public ServiceBusProcessorClient startMessageProcessor(String topicName, String subscriptionName) {
        log.info("starting service bus processor {}/{}", topicName, subscriptionName);
        ServiceBusClientBuilder.ServiceBusProcessorClientBuilder processorBuilder = configService
                .clientBuilder()
                .processor()
                .topicName(topicName)
                .subscriptionName(subscriptionName)
                .processMessage(context -> handleMessage(topicName, subscriptionName, context))
                .processError(context -> handleError(topicName, subscriptionName));
        ServiceBusProcessorClient processorClient = processorBuilder.buildProcessorClient();
        processorClient.start();
        return processorClient;
    }

    public void handleMessage(String topicName, String subscriptionName, ServiceBusReceivedMessageContext context) {
        ServiceBusMessageWrapper queueMessage = jsonMapper.fromJson(String.valueOf(context.getMessage().getBody()), ServiceBusMessageWrapper.class);
        log.info("Processing {}/{}", topicName, subscriptionName);
        try {
            // notificationMapper.mapToPayload()
            callbackClient.sendNotification("url-for-sub", null);
            // remoteClientService.receiveMessage(topicName, subscriptionName, queueMessage.getMessage());
        } catch (Exception exception) {
            int failCount = queueMessage.getFailureCount() + 1;
            log.error("handleMessage failuerCount:{} with exception.", failCount, exception);
            if (failCount >= configService.getMaxTries()) {
                log.error("handleMessage failed finally");
                throw exception;
            }
            queueMessage(topicName, queueMessage.getMessage(), failCount);
            // Because we added a new message and swallowed the error then the current message will be dropped
        }
    }

    public void handleError(String topicName, String subscriptionName) {
        // We should only be called when failCount has exceeded maxTries and message go to DLQ
        log.error("handleError unexpected error on {}/{} moving to DLQ", topicName, subscriptionName);
    }
}
