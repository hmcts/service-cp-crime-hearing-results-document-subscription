package uk.gov.hmcts.cp.servicebus.integration;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.administration.models.SubscriptionProperties;
import com.azure.messaging.servicebus.administration.models.TopicProperties;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.servicebus.config.ServiceBusConfigService;
import uk.gov.hmcts.cp.servicebus.services.ServiceBusService;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ServiceBusTestService {

    @Autowired
    ServiceBusConfigService configService;
    @Autowired
    ServiceBusService topicService;

    public boolean isServiceBusReady() {
        try {
            configService.adminClient().listTopics().stream().map(TopicProperties::getName).toList();
            return true;
        } catch (Exception e) {
            log.info("waiting for servicebus to start");
            return false;
        }
    }

    public void dropTopicIfExists(String topicName, String subscriptionName) {
        List<String> subscriptions = configService.adminClient().listSubscriptions("topic.new").stream().map(SubscriptionProperties::getSubscriptionName).toList();
        if (subscriptions.contains(subscriptionName)) {
            configService.adminClient().deleteSubscription(topicName, subscriptionName);
        }
        List<String> topics = configService.adminClient().listTopics().stream().map(TopicProperties::getName).toList();
        if (topics.contains(topicName)) {
            configService.adminClient().deleteTopic(topicName);
        }
    }

    // Rather then purge messages for Tests we are probably better just dropping the topic and subscription
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
