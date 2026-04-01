package uk.gov.hmcts.cp.servicebus.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.cp.servicebus.services.ServiceBusAdminService;
import uk.gov.hmcts.cp.servicebus.services.ServiceBusClientService;
import uk.gov.hmcts.cp.servicebus.services.ServiceBusProcessorService;
import uk.gov.hmcts.cp.subscription.services.JsonMapper;

import static org.junit.jupiter.api.Assumptions.abort;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Slf4j
@SpringBootTest
public class ServiceBusIntegrationTestBase {

    @Autowired
    protected ServiceBusAdminService adminService;
    @Autowired
    protected JsonMapper jsonMapper;
    @Autowired
    protected ServiceBusClientService clientService;
    @Autowired
    protected ServiceBusProcessorService processorService;
    @Autowired
    protected ServiceBusTestService testService;

    protected void prepareQueue(final String queueName) {
        assumeTrue(adminService.isServiceBusReady(),
                "ServiceBus is not running. Run ./gradlew dockerTest or ./gradlew composeUp before these tests.");
        try {
            processorService.stopMessageProcessor(queueName);
            testService.dropQueueIfExists(queueName);
            adminService.createQueue(queueName);
            processorService.startMessageProcessor(queueName);
        } catch (Exception e) {
            abort("Service Bus setup failed — run ./gradlew dockerTest or composeUp");
        }
    }
}
