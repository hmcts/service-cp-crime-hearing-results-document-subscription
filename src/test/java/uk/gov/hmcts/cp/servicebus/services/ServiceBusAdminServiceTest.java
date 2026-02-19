package uk.gov.hmcts.cp.servicebus.services;

import com.azure.core.http.rest.PagedIterable;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClient;
import com.azure.messaging.servicebus.administration.models.CreateSubscriptionOptions;
import com.azure.messaging.servicebus.administration.models.CreateTopicOptions;
import com.azure.messaging.servicebus.administration.models.SubscriptionProperties;
import com.azure.messaging.servicebus.administration.models.TopicProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cp.subscription.config.ServiceBusConfigService;

import java.time.Duration;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceBusAdminServiceTest {

    @Mock
    ServiceBusConfigService configService;

    @InjectMocks
    ServiceBusAdminService adminService;

    @Mock
    ServiceBusAdministrationClient serviceBusAdministrationClient;
    @Mock
    PagedIterable<TopicProperties> topics;
    @Mock
    PagedIterable<SubscriptionProperties> subscriptions;
    @Mock
    TopicProperties topicProperties;
    @Mock
    SubscriptionProperties subscriptionProperties;

    @Captor
    ArgumentCaptor<CreateTopicOptions> topicCaptor;
    @Captor
    ArgumentCaptor<CreateSubscriptionOptions> subscriptionCaptor;

    @Test
    void service_bus_started_should_return_true() {
        when(configService.adminClient()).thenReturn(serviceBusAdministrationClient);
        when(serviceBusAdministrationClient.listTopics()).thenReturn(topics);

        assertThat(adminService.isServiceBusReady()).isTrue();
    }

    @Test
    void service_bus_down_should_throw() {
        when(configService.adminClient()).thenThrow(new RuntimeException("Connection refused"));
        assertThat(adminService.isServiceBusReady()).isFalse();
    }

    @Test
    void create_should_create_topic_and_subscription() {
        when(configService.adminClient()).thenReturn(serviceBusAdministrationClient);
        when(serviceBusAdministrationClient.listTopics()).thenReturn(topics);
        when(serviceBusAdministrationClient.listSubscriptions("topic1")).thenReturn(subscriptions);

        adminService.createTopicAndSubscription("topic1", "subscription1");

        verify(serviceBusAdministrationClient).createTopic(eq("topic1"), topicCaptor.capture());
        assertThat(topicCaptor.getValue().getDefaultMessageTimeToLive()).isEqualTo(Duration.ofHours(1));

        verify(serviceBusAdministrationClient).createSubscription(eq("topic1"), eq("subscription1"), subscriptionCaptor.capture());
        assertThat(subscriptionCaptor.getValue().getLockDuration()).isEqualTo(Duration.ofMinutes(1));
        assertThat(subscriptionCaptor.getValue().getMaxDeliveryCount()).isEqualTo(1);
    }

    @Test
    void create_should_skip_topic_and_subscription_if_exists() {
        when(configService.adminClient()).thenReturn(serviceBusAdministrationClient);
        when(serviceBusAdministrationClient.listTopics()).thenReturn(topics);
        when(topics.stream()).thenReturn(Stream.of(topicProperties));
        when(topicProperties.getName()).thenReturn("topic1");
        when(serviceBusAdministrationClient.listSubscriptions("topic1")).thenReturn(subscriptions);
        when(subscriptions.stream()).thenReturn(Stream.of(subscriptionProperties));
        when(subscriptionProperties.getSubscriptionName()).thenReturn("subscription1");

        adminService.createTopicAndSubscription("topic1", "subscription1");

        verify(serviceBusAdministrationClient, never()).createTopic(eq("topic1"), topicCaptor.capture());
        verify(serviceBusAdministrationClient, never()).createSubscription(eq("topic1"), eq("subscription1"), subscriptionCaptor.capture());
    }
}