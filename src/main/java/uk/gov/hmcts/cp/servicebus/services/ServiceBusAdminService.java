package uk.gov.hmcts.cp.servicebus.services;

import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClient;
import com.azure.messaging.servicebus.administration.models.CreateSubscriptionOptions;
import com.azure.messaging.servicebus.administration.models.CreateTopicOptions;
import com.azure.messaging.servicebus.administration.models.SubscriptionProperties;
import com.azure.messaging.servicebus.administration.models.TopicProperties;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.subscription.config.ServiceBusConfigService;

import java.time.Duration;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class ServiceBusAdminService {

    private final ServiceBusConfigService configService;

    @SuppressWarnings({"PMD.AvoidCatchingGenericException","PMD.OnlyOneReturn"})
    public boolean isServiceBusReady() {
        try {
            configService.adminClient().listTopics().stream().map(TopicProperties::getName).toList();
            return true;
        } catch (Exception e) {
            log.info("waiting for servicebus to be available");
            return false;
        }
    }

    public void createTopicAndSubscription(final String topicName, final String subscriptionName) {
        final ServiceBusAdministrationClient adminClient = configService.adminClient();
        final List<String> topics = adminClient.listTopics().stream().map(TopicProperties::getName).toList();
        if (topics.contains(topicName)) {
            log.info("Topic {} already exists", topicName);
        } else {
            log.info("Creating topic {}", topicName);
            final CreateTopicOptions createTopicOptions = new CreateTopicOptions();
            createTopicOptions.setDefaultMessageTimeToLive(Duration.ofHours(1));
            adminClient.createTopic(topicName, createTopicOptions);
        }

        final List<String> subscriptions = adminClient.listSubscriptions(topicName).stream().map(SubscriptionProperties::getSubscriptionName).toList();
        if (subscriptions.contains(subscriptionName)) {
            log.info("Subscription {}/{} already exists", topicName, subscriptionName);
        } else {
            final CreateSubscriptionOptions options = new CreateSubscriptionOptions();
            options.setLockDuration(Duration.ofMinutes(1));
            options.setMaxDeliveryCount(1);
            adminClient.createSubscription(topicName, subscriptionName, options);
        }
    }
}
