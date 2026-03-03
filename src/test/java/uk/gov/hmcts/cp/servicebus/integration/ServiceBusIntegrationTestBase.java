package uk.gov.hmcts.cp.servicebus.integration;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.administration.models.TopicProperties;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.cp.servicebus.config.ServiceBusConfigService;
import uk.gov.hmcts.cp.servicebus.services.ServiceBusService;

import java.util.concurrent.TimeUnit;

@Slf4j
@SpringBootTest
public class ServiceBusIntegrationTestBase {

    @Autowired
    ServiceBusConfigService configService;
    @Autowired
    ServiceBusService topicService;

    String topicName = "topic.1";
    String subscription1 = "subscription.1";
    String subscription2 = "subscription.2";
    int maxDeliveryCount = 3;
    String message = "My message";

    protected boolean isServiceBusReady() {
        try {
            configService.adminClient().listTopics().stream().map(TopicProperties::getName).toList();
            return true;
        } catch (Exception e) {
            log.info("waiting for servicebus to start");
            return false;
        }
    }

    @SneakyThrows
    public void purgeMessages(String topicName, String subscriptionName) {
        log.info("purgeMessages {}/{}", topicName, subscriptionName);
        ServiceBusClientBuilder.ServiceBusProcessorClientBuilder processorBuilder = configService.processorClientBuilder(topicName, subscriptionName);
        processorBuilder.processMessage(context -> purgeMessage(topicName, subscriptionName, context));
        processorBuilder.processError(context -> topicService.handleError(topicName, subscriptionName));

        ServiceBusProcessorClient processor = processorBuilder.buildProcessorClient();
        processor.start();
        TimeUnit.MILLISECONDS.sleep(100);
        processor.stop();

        log.info("purgeMessages DLQ {}/{}", topicName, subscriptionName);
        ServiceBusClientBuilder.ServiceBusProcessorClientBuilder processorDlqBuilder = configService.processorClientBuilder(topicName, subscriptionName);
        processorDlqBuilder.processMessage(context -> purgeMessage(topicName, subscriptionName, context));
        processorDlqBuilder.processError(context -> topicService.handleError(topicName, subscriptionName));

        ServiceBusProcessorClient processorDlq = processorDlqBuilder.buildProcessorClient();
        processorDlq.start();
        TimeUnit.MILLISECONDS.sleep(100);
        processorDlq.stop();
    }

    private void purgeMessage(String topicName, String subscriptionName, ServiceBusReceivedMessageContext context) {
        log.info("purgeMessage {}/{} {}", topicName, subscriptionName, context.getMessage().getMessageId());
        // do nothing just let the message complete
    }
}
