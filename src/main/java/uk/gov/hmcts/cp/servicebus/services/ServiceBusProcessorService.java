package uk.gov.hmcts.cp.servicebus.services;

import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.servicebus.config.ServiceBusConfigService;
import uk.gov.hmcts.cp.servicebus.model.ServiceBusWrappedMessage;
import uk.gov.hmcts.cp.subscription.services.JsonMapper;

@Service
@AllArgsConstructor
@Slf4j
public class ServiceBusProcessorService {

    private final ServiceBusConfigService configService;
    private final ServiceBusClientService clientService;
    private final JsonMapper jsonMapper;
    private final ServiceBusHandlers serviceBusHandlers;

    @SneakyThrows
    public void startMessageProcessor(final String topicName) {
        log.info("starting service bus processor {}/{}", topicName, topicName);
        final ServiceBusProcessorClient processorClient = configService
                .processorClientBuilder(topicName, topicName)
                .processMessage(context -> handleMessage(topicName, context))
                .processError(context -> handleError(topicName, context))
                .buildProcessorClient();
        processorClient.start();
    }

    public void handleMessage(final String topicName, final ServiceBusReceivedMessageContext context) {
        final String wrappedMessageString = String.valueOf(context.getMessage().getBody());
        final ServiceBusWrappedMessage queueMessage = jsonMapper.fromJson(wrappedMessageString, ServiceBusWrappedMessage.class);
        log.info("Processing {} with targetUrl:{}", topicName, queueMessage.getTargetUrl());
        try {
            serviceBusHandlers.handleMessage(topicName, queueMessage.getTargetUrl(), queueMessage.getMessage());
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

    public void handleError(final String topicName, final ServiceBusErrorContext errorContext) {
        // We should only be called when failCount has exceeded maxTries and message go to DLQ
        log.error("handleError unexpected error on {} moving to DLQ", topicName, errorContext.getException());
    }
}
