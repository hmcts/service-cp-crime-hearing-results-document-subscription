package uk.gov.hmcts.cp.servicebus.integration;

import com.azure.messaging.servicebus.administration.models.SubscriptionProperties;
import com.azure.messaging.servicebus.administration.models.TopicProperties;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.cp.servicebus.services.ServiceBusAdminService;
import uk.gov.hmcts.cp.subscription.config.ServiceBusConfigService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
@Disabled // Run manually untIl we add the docker compose gradle up ... or sort TestContainers would be better
public class ServiceBusAdminIntegrationTest {

    @Autowired
    ServiceBusConfigService configService;
    @Autowired
    ServiceBusAdminService adminService;

    @BeforeEach
    void beforeEach() {
        if (adminService.isServiceBusReady()) {
            log.info("ServiceBus is up and running");
        } else {
            throw new RuntimeException("ServiceBus is not running. Please start it in docker-compose");
        }
    }

    @Test
    void admin_console_should_create_new_topic_and_subscription_just_once() {
        adminService.createTopicAndSubscription("topic.new", "subscription.new");
        List<String> topics = configService.adminClient().listTopics().stream().map(TopicProperties::getName).toList();
        assertThat(topics.contains("topic.new"));
        List<String> subscriptions = configService.adminClient().listSubscriptions("topic.new").stream().map(SubscriptionProperties::getSubscriptionName).toList();
        assertThat(subscriptions.contains("subscription.new"));

        adminService.createTopicAndSubscription("topic.new", "subscription.new");
        configService.adminClient().deleteSubscription("topic.new", "subscription.new");
        configService.adminClient().deleteTopic("topic.new");
    }
}
