package uk.gov.hmcts.cp.servicebus.services;

import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.servicebus.config.ServiceBusConfigService;
import uk.gov.hmcts.cp.servicebus.model.ServiceBusWrappedMessage;
import uk.gov.hmcts.cp.subscription.services.JsonMapper;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.awaitility.Awaitility.await;
import static uk.gov.hmcts.cp.filters.TracingFilter.CORRELATION_ID_KEY;
import static uk.gov.hmcts.cp.servicebus.config.ServiceBusConfigService.PCR_INBOUND_TOPIC;
import static uk.gov.hmcts.cp.servicebus.config.ServiceBusConfigService.PCR_OUTBOUND_TOPIC;

@Service
@AllArgsConstructor
@Slf4j
public class ServiceBusProcessorService {

    private static final int MAX_WAIT_SECONDS = 120;
    private static final int POLL_SECONDS = 10;

    private final ServiceBusAdminService adminService;
    private final ServiceBusConfigService configService;
    private final ServiceBusClientService clientService;
    private final JsonMapper jsonMapper;
    private final ServiceBusHandlers serviceBusHandlers;

    private Map<String, ServiceBusProcessorClient> processorClients = new HashMap<>();

    @PostConstruct
    public void initialiseServiceBus() {
        if (configService.isEnabled()) {
            try {
                await()
                        .atMost(Duration.ofSeconds(MAX_WAIT_SECONDS))
                        .pollInterval(Duration.ofSeconds(POLL_SECONDS))
                        .until(adminService::isServiceBusReady);
                log.info("createServiceBusQueues creating service bus queues");
                adminService.createTopicAndSubscription(PCR_INBOUND_TOPIC);
                startMessageProcessor(PCR_INBOUND_TOPIC);
                adminService.createTopicAndSubscription(PCR_OUTBOUND_TOPIC);
                startMessageProcessor(PCR_OUTBOUND_TOPIC);
            } catch (Exception e) {
                log.error("Failed to initialise serviceBus. {}", e.getMessage());
            }
        }
    }

    public void stopMessageProcessor(final String topicName) {
        final ServiceBusProcessorClient processorClient = processorClients.get(topicName);
        if (processorClient != null && processorClient.isRunning()) {
            log.info("Service Bus Processor {} is being stopped", topicName);
            processorClient.stop();
            processorClients.remove(topicName);
        } else {
            log.info("Service Bus Processor {} is not running", topicName);
        }
    }

    @SneakyThrows
    public void startMessageProcessor(final String topicName) {
        log.info("starting service bus processor {}/{}", topicName, topicName);
        final ServiceBusProcessorClient processorClient = configService
                .processorClientBuilder(topicName, topicName)
                .processMessage(context -> handleMessage(topicName, context))
                .processError(context -> handleError(topicName, context))
                .buildProcessorClient();
        processorClient.start();
        processorClients.put(topicName, processorClient);
    }

    public void handleMessage(final String topicName, final ServiceBusReceivedMessageContext context) {
        final String wrappedMessageString = context.getMessage().getBody().toString();
        final ServiceBusWrappedMessage queueMessage = jsonMapper.fromJson(wrappedMessageString, ServiceBusWrappedMessage.class);
        log.info("Processing {} with targetUrl:{}", topicName, queueMessage.getTargetUrl());
        try {
            MDC.put(CORRELATION_ID_KEY, queueMessage.getCorrelationId().toString());
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
        } finally {
            MDC.remove(CORRELATION_ID_KEY);
        }
    }

    public void handleError(final String topicName, final ServiceBusErrorContext errorContext) {
        // We should only be called when failCount has exceeded maxTries and message go to DLQ
        log.error("handleError unexpected error on {} moving to DLQ", topicName, errorContext.getException());
    }
}
