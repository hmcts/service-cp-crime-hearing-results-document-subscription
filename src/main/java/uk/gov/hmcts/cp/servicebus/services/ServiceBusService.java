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
import uk.gov.hmcts.cp.notification.clients.CallbackClient;
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

    @SneakyThrows
    public ServiceBusProcessorClient startMessageProcessor(final String topicName, final String subscriptionName) {
        log.info("starting service bus processor {}/{}", topicName, subscriptionName);
        final ServiceBusClientBuilder.ServiceBusProcessorClientBuilder processorBuilder = configService
                .clientBuilder()
                .processor()
                .topicName(topicName)
                .subscriptionName(subscriptionName)
                .processMessage(context -> handleMessage(topicName, subscriptionName, context))
                .processError(context -> handleError(topicName, subscriptionName));
        final ServiceBusProcessorClient processorClient = processorBuilder.buildProcessorClient();
        processorClient.start();
        return processorClient;
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public void handleMessage(final String topicName, final String subscriptionName, final ServiceBusReceivedMessageContext context) {
        final ServiceBusMessageWrapper queueMessage = jsonMapper.fromJson(String.valueOf(context.getMessage().getBody()), ServiceBusMessageWrapper.class);
        log.info("Processing {}/{}", topicName, subscriptionName);
        try {
            log.info("handleMessage url:{} with message:{}", queueMessage.getTargetUrl(), queueMessage.getMessage());
            final EventNotificationPayload callbackPayload = jsonMapper.fromJson(queueMessage.getMessage(), EventNotificationPayload.class);
            callbackClient.sendNotification(queueMessage.getTargetUrl(), callbackPayload);
        } catch (Exception exception) {
            final int failCount = queueMessage.getFailureCount() + 1;
            log.error("handleMessage failuerCount:{} with exception.", failCount, exception);
            if (failCount >= configService.getMaxTries()) {
                log.error("handleMessage failed finally");
                throw exception;
            }
            queueMessage(topicName, queueMessage.getTargetUrl(), queueMessage.getMessage(), failCount);
            // Because we added a new message and swallowed the error then the current message will be dropped
        }
    }

    public void handleError(final String topicName, final String subscriptionName) {
        // We should only be called when failCount has exceeded maxTries and message go to DLQ
        log.error("handleError unexpected error on {}/{} moving to DLQ", topicName, subscriptionName);
    }
}
