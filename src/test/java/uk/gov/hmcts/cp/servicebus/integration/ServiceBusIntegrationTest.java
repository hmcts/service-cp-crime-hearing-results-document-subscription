package uk.gov.hmcts.cp.servicebus.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;
import uk.gov.hmcts.cp.openapi.model.EventNotificationPayload;
import uk.gov.hmcts.cp.servicebus.services.ServiceBusAdminService;
import uk.gov.hmcts.cp.servicebus.services.ServiceBusService;
import uk.gov.hmcts.cp.subscription.config.IgnoreSSLCertificatesForWiremockTest;
import uk.gov.hmcts.cp.subscription.integration.config.TestContainersInitialise;
import uk.gov.hmcts.cp.subscription.services.JsonMapper;

import java.time.Duration;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static uk.gov.hmcts.cp.subscription.integration.stubs.CallbackStub.stubCallbackEndpoint;

@Slf4j
@SpringBootTest
@ContextConfiguration(initializers = TestContainersInitialise.class)
@EnableWireMock({
        @ConfigureWireMock(name = "material-client", baseUrlProperties = "material-client.url", port = 0),
        @ConfigureWireMock(name = "callback-client", httpsBaseUrlProperties = "callback-client.url", httpsPort = 0)
})
@Import(IgnoreSSLCertificatesForWiremockTest.class)
@Disabled // to do
public class ServiceBusIntegrationTest extends ServiceBusIntegrationTestBase {

    @Autowired
    JsonMapper jsonMapper;
    @Autowired
    ServiceBusAdminService adminService;
    @Autowired
    ServiceBusService topicService;

    @InjectWireMock("callback-client")
    private WireMockServer callbackWireMockServer;

    @BeforeEach
    void setUp() {
        await()
                .atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofSeconds(1))
                .until(this::isServiceBusReady);
        adminService.createTopicAndSubscription(topicName, subscription1);
        adminService.createTopicAndSubscription(topicName, subscription2);
        purgeMessages(topicName, subscription1);
        purgeMessages(topicName, subscription2);

        topicService.startMessageProcessor(topicName, subscription1);
        topicService.startMessageProcessor(topicName, subscription2);
    }

    @SneakyThrows
    @Test
    void sent_messages_should_process_and_send_to_client() {
        String targetUrl = "http://localhost/callback";
        stubCallbackEndpoint(callbackWireMockServer, targetUrl);
        // UUID is 8-4-4-4-12 = 32+4
        UUID documentId = UUID.fromString("11111111-1111-1111-1111-111122223333");
        EventNotificationPayload message = EventNotificationPayload.builder()
                .documentId(documentId)
                .build();
        topicService.queueMessage(topicName, targetUrl, jsonMapper.toJson(message), 0);

        Thread.sleep(10000);
    }

//    @Test
//    void process_message_should_retry_n_times_then_send_to_DLQ() {
//        topicService.queueMessage(topicName, message, 0);
//
//        log.info("getting messages ... {} sends and then should fail to DLQ", maxDeliveryCount);
//        doThrow(HttpClientErrorException.class).when(remoteClientService).receiveMessage(topicName, subscription1, message);
//        // topicService.processMessages(topicName, subscription1, 500);
//        verify(remoteClientService, times(maxDeliveryCount)).receiveMessage(topicName, subscription1, message);
//
//        log.info("reprocessing messages from DLQ just once");
//        // reset(remoteClientService);
//        // topicService.processDeadLetterMessages(topicName, subscription1, 500);
//        // verify(remoteClientService).receiveMessage(topicName, subscription1 + "-DLQ", message);
//    }

}
