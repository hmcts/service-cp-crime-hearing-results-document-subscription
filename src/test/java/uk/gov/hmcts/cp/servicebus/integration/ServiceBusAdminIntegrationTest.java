package uk.gov.hmcts.cp.servicebus.integration;

import com.azure.messaging.servicebus.administration.models.SubscriptionProperties;
import com.azure.messaging.servicebus.administration.models.TopicProperties;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.cp.servicebus.config.ServiceBusConfigService;
import uk.gov.hmcts.cp.servicebus.services.ServiceBusAdminService;
import uk.gov.hmcts.cp.subscription.integration.config.TestContainersInitialise;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Slf4j
@SpringBootTest
@ContextConfiguration(initializers = TestContainersInitialise.class)
public class ServiceBusAdminIntegrationTest {

    @Autowired
    ServiceBusTestService testService;
    @Autowired
    ServiceBusConfigService configService;
    @Autowired
    ServiceBusAdminService adminService;

    @BeforeEach
    void beforeEach() {
        assumeTrue(adminService.isServiceBusReady(), "ServiceBus is not running. Run gradlew composeUp / composeDown");
        log.info("ServiceBus is up and running");
    }

    @Test
    void admin_client_should_create_new_topic_and_subscription_and_delete() {
        testService.dropTopicIfExists("topic.new", "subscription.new");
        adminService.createTopicAndSubscription("topic.new", "subscription.new");
        List<String> topics = configService.adminClient().listTopics().stream().map(TopicProperties::getName).toList();
        assertThat(topics.contains("topic.new"));
        List<String> subscriptions = configService.adminClient().listSubscriptions("topic.new").stream().map(SubscriptionProperties::getSubscriptionName).toList();
        assertThat(subscriptions.contains("subscription.new"));

        adminService.createTopicAndSubscription("topic.new", "subscription.new");
        testService.dropTopicIfExists("topic.new", "subscription.new");
    }
}
