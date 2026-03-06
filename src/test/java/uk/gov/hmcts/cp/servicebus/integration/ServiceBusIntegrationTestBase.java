package uk.gov.hmcts.cp.servicebus.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.cp.servicebus.config.ServiceBusConfigService;
import uk.gov.hmcts.cp.servicebus.services.ServiceBusAdminService;
import uk.gov.hmcts.cp.servicebus.services.ServiceBusClientService;
import uk.gov.hmcts.cp.servicebus.services.ServiceBusProcessorService;
import uk.gov.hmcts.cp.subscription.services.JsonMapper;

@Slf4j
@SpringBootTest
public class ServiceBusIntegrationTestBase {

    @Autowired
    ServiceBusConfigService configService;
    @Autowired
    ServiceBusAdminService adminService;
    @Autowired
    JsonMapper jsonMapper;
    @Autowired
    ServiceBusClientService clientService;
    @Autowired
    ServiceBusProcessorService processorService;
    @Autowired
    ServiceBusTestService testService;

    String subscription1 = "subscription.1";
}
