package uk.gov.hmcts.cp.servicebus.services;

import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.openapi.model.EventNotificationPayload;
import uk.gov.hmcts.cp.openapi.model.PcrEventPayload;
import uk.gov.hmcts.cp.servicebus.config.ServiceBusConfigService;
import uk.gov.hmcts.cp.servicebus.model.ServiceBusMessageWrapper;
import uk.gov.hmcts.cp.subscription.clients.CallbackClient;
import uk.gov.hmcts.cp.subscription.managers.NotificationManager;
import uk.gov.hmcts.cp.subscription.services.JsonMapper;

import static uk.gov.hmcts.cp.servicebus.config.ServiceBusConfigService.PCR_INBOUND_TOPIC;
import static uk.gov.hmcts.cp.servicebus.config.ServiceBusConfigService.PCR_OUTBOUND_TOPIC;

@Service
@AllArgsConstructor
@Slf4j
public class ServiceBusProcessorService {

    private final ServiceBusConfigService configService;
    private final CallbackClient callbackClient;
    private final NotificationManager notificationManager;
    private final ServiceBusClientService clientService;
    private final JsonMapper jsonMapper;

    @SneakyThrows
    public ServiceBusProcessorClient startMessageProcessor(final String topicName, final String subscriptionName) {
        log.info("starting service bus processor {}/{}", topicName, subscriptionName);
        final ServiceBusProcessorClient processorClient = configService
                .processorClientBuilder(topicName, subscriptionName)
                .processMessage(context -> handleMessage(topicName, subscriptionName, context))
                .processError(context -> handleError(topicName, subscriptionName, context))
                .buildProcessorClient();
        processorClient.start();
        return processorClient;
    }

    public void handleMessage(final String topicName, final String subscriptionName, final ServiceBusReceivedMessageContext context) {
        final String wrappedMessageString = String.valueOf(context.getMessage().getBody());
        final ServiceBusMessageWrapper queueMessage = jsonMapper.fromJson(wrappedMessageString, ServiceBusMessageWrapper.class);
        log.info("Processing {} with targetUrl:{}", topicName, queueMessage.getTargetUrl());
        try {
            handleMessageType(topicName, queueMessage.getTargetUrl(), queueMessage.getMessage());
        } catch (Exception exception) {
            final int failureCount = queueMessage.getFailureCount() + 1;
            log.error("handleMessage failureCount:{} of {} tries with exception.", failureCount, configService.getMaxTries(), exception);
            if (failureCount >= configService.getMaxTries()) {
                log.error("handleMessage failed finally");
                throw exception;
            }
            clientService.queueMessage(topicName, queueMessage.getTargetUrl(), queueMessage.getMessage(), failureCount);
            // Because we added a new message and swallowed the error then the current message will be dropped
        }
    }

    private void handleMessageType(final String topicName, final String target, final String message) {
        log.info("handleMessageType {} message:{}", topicName, message);
        switch (topicName) {
            case PCR_INBOUND_TOPIC -> {
                final PcrEventPayload pcrEventPayload = jsonMapper.fromJson(message, PcrEventPayload.class);
                notificationManager.processPcrNotification(pcrEventPayload);
            }
            case PCR_OUTBOUND_TOPIC -> {
                final EventNotificationPayload eventNotificationPayload = jsonMapper.fromJson(message, EventNotificationPayload.class);
                callbackClient.sendNotification(target, eventNotificationPayload);
            }
            default -> throw new RuntimeException("Invalid topic name " + topicName);
        }
        log.info("handleMessageType completed OK");
    }

    public void handleError(final String topicName, final String subscriptionName, final ServiceBusErrorContext errorContext) {
        // We should only be called when failCount has exceeded maxTries and message go to DLQ
        log.error("handleError unexpected error on {}/{} moving to DLQ", topicName, subscriptionName, errorContext.getException());
    }
}
