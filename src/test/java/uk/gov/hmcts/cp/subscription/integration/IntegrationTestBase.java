package uk.gov.hmcts.cp.subscription.integration;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.cp.servicebus.services.ServiceBusProcessorService;

/**
 * Default integration tests: Service Bus message processors are mocked so the suite does not require
 * a running emulator. Use {@link AbstractSubscriptionIntegrationTest} when a real processor is required.
 */
public abstract class IntegrationTestBase extends AbstractSubscriptionIntegrationTest {

    @MockitoBean
    @SuppressWarnings("unused")
    private ServiceBusProcessorService serviceBusProcessorService;
}
