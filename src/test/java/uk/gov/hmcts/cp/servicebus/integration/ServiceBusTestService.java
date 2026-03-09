package uk.gov.hmcts.cp.servicebus.integration;

import com.azure.messaging.servicebus.administration.models.SubscriptionProperties;
import com.azure.messaging.servicebus.administration.models.TopicProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.servicebus.config.ServiceBusConfigService;

import java.util.List;

@Slf4j
@Service
public class ServiceBusTestService {

    @Autowired
    ServiceBusConfigService configService;

    public boolean isServiceBusReady() {
        try {
            configService.adminClient().listTopics().stream().map(TopicProperties::getName).toList();
            return true;
        } catch (Exception e) {
            log.info("waiting for servicebus to start");
            return false;
        }
    }

    public void dropTopicIfExists(String topicName) {
        List<String> subscriptions = configService.adminClient().listSubscriptions("topic.new").stream().map(SubscriptionProperties::getSubscriptionName).toList();
        for (String subscriptionName : subscriptions) {
            configService.adminClient().deleteSubscription(topicName, subscriptionName);
        }
        List<String> topics = configService.adminClient().listTopics().stream().map(TopicProperties::getName).toList();
        if (topics.contains(topicName)) {
            configService.adminClient().deleteTopic(topicName);
        }
    }
}
