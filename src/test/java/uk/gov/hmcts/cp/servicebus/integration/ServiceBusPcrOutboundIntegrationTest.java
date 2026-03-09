package uk.gov.hmcts.cp.servicebus.integration;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.cp.openapi.model.EventNotificationPayload;
import uk.gov.hmcts.cp.subscription.clients.CallbackClient;
import uk.gov.hmcts.cp.subscription.integration.config.TestContainersInitialise;

import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.cp.servicebus.config.ServiceBusConfigService.PCR_OUTBOUND_TOPIC;

@Slf4j
@SpringBootTest
@ContextConfiguration(initializers = TestContainersInitialise.class)
@TestPropertySource(properties = {
        "service-bus.max-tries=2",
        "service-bus.retry-msecs=0"
})
public class ServiceBusPcrOutboundIntegrationTest extends ServiceBusIntegrationTestBase {

    @MockitoBean
    CallbackClient callbackClient;

    @BeforeEach
    void setUp() {
        await()
                .atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofSeconds(1))
                .until(testService::isServiceBusReady);
        testService.dropTopicIfExists(PCR_OUTBOUND_TOPIC);
        adminService.createTopicAndSubscription(PCR_OUTBOUND_TOPIC);
        processorService.startMessageProcessor(PCR_OUTBOUND_TOPIC);
    }

    @SneakyThrows
    @Test
    void sent_messages_should_process_and_send_to_client() {
        for (int n = 0; n < 10; n++) {
            String callbackUrl = String.format("http://callback%d", n);
            queueMessageForCallbackUrl(callbackUrl);
        }
        Thread.sleep(2000);
        for (int n = 0; n < 10; n++) {
            String callbackUrl = String.format("http://callback%d", n);
            verify(callbackClient).sendNotification(eq(callbackUrl), any(EventNotificationPayload.class));
        }
    }

    @SneakyThrows
    @Test
    void process_message_should_retry_n_times_then_stop_because_sent_to_DLQ() {
        doThrow(new RuntimeException("Error")).when(callbackClient).sendNotification(eq("http://callback"), any(EventNotificationPayload.class));
        queueMessageForCallbackUrl("http://callback");
        Thread.sleep(4000);
        verify(callbackClient, times(2)).sendNotification(eq("http://callback"), any(EventNotificationPayload.class));
    }

    private void queueMessageForCallbackUrl(String callbackUrl) {
        EventNotificationPayload payload = EventNotificationPayload.builder().build();
        clientService.queueMessage(PCR_OUTBOUND_TOPIC, callbackUrl, jsonMapper.toJson(payload), 0);
    }
}
