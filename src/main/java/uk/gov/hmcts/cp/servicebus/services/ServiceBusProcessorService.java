package uk.gov.hmcts.cp.servicebus.services;

import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.openapi.model.EventNotificationPayload;
import uk.gov.hmcts.cp.servicebus.config.ServiceBusConfigService;
import uk.gov.hmcts.cp.servicebus.mapper.ServiceBusMapper;
import uk.gov.hmcts.cp.servicebus.model.ServiceBusMessageWrapper;
import uk.gov.hmcts.cp.notification.clients.CallbackClient;
import uk.gov.hmcts.cp.notification.mappers.NotificationMapper;
import uk.gov.hmcts.cp.subscription.services.JsonMapper;

@Service
@AllArgsConstructor
@Slf4j
public class ServiceBusProcessorService {

    private final ServiceBusConfigService configService;
    private final NotificationMapper notificationMapper;
    private final ServiceBusMapper serviceBusMapper;
    private final CallbackClient callbackClient;
    private final ServiceBusClientService clientService;
    private final JsonMapper jsonMapper;

    @SneakyThrows
    public ServiceBusProcessorClient startMessageProcessor(final String topicName, final String subscriptionName) {
        log.info("starting service bus processor {}/{}", topicName, subscriptionName);
        final ServiceBusProcessorClient processorClient = configService
                .processorClientBuilder(topicName, subscriptionName)
                .processMessage(context -> handleMessage(topicName, subscriptionName, context))
                .processError(context -> handleError(topicName, subscriptionName))
                .buildProcessorClient();
        processorClient.start();
        return processorClient;
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public void handleMessage(final String topicName, final String subscriptionName, final ServiceBusReceivedMessageContext context) {
        final String message = String.valueOf(context.getMessage().getBody());
        final ServiceBusMessageWrapper wrappedMessage = jsonMapper.fromJson(message, ServiceBusMessageWrapper.class);
        log.info("Processing {}/{}", topicName, subscriptionName);
        try {
            final EventNotificationPayload payload = notificationMapper.mapFromJson(wrappedMessage.getMessage());
            callbackClient.sendNotification(wrappedMessage.getTargetUrl(), payload);
        } catch (Exception exception) {
            final int failCount = wrappedMessage.getFailureCount() + 1;
            log.error("handleMessage failCount:{}/{} with exception.", failCount, configService.getMaxTries(), exception);
            if (failCount >= configService.getMaxTries()) {
                log.error("handleMessage failed finally");
                throw exception;
            }
            clientService.queueMessage(topicName, wrappedMessage.getMessage(), failCount);
            // Because we added a new message and swallowed the error then the current message will be silently dropped
        }
    }

    public void handleError(final String topicName, final String subscriptionName) {
        // We should only be called when failCount has exceeded maxTries and message go to DLQ
        log.error("handleError unexpected error on {}/{} moving to DLQ", topicName, subscriptionName);
    }
}
