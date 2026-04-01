package uk.gov.hmcts.cp.servicebus.services;

import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.cp.servicebus.config.ServiceBusProperties;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceBusClientFactoryTest {

    private static final String EMULATOR_ADMIN =
            "Endpoint=sb://localhost:5300;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=SAS_KEY_VALUE;UseDevelopmentEmulator=true;";
    private static final String EMULATOR_MESSAGING =
            "Endpoint=sb://localhost;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=SAS_KEY_VALUE;UseDevelopmentEmulator=true;";

    @Test
    void senderClient_buildsNonNullClientUsingMessagingConnectionString() {
        final ServiceBusProperties properties = new ServiceBusProperties(false, EMULATOR_ADMIN, EMULATOR_MESSAGING, 5);
        final ServiceBusClientFactory factory = new ServiceBusClientFactory(properties);

        try (ServiceBusSenderClient sender = factory.senderClient("test-queue")) {
            assertThat(sender).isNotNull();
        }
    }

    @Test
    void processorClientBuilder_returnsNonNullBuilderForQueue() {
        final ServiceBusProperties properties = new ServiceBusProperties(false, EMULATOR_ADMIN, EMULATOR_MESSAGING, 5);
        final ServiceBusClientFactory factory = new ServiceBusClientFactory(properties);

        assertThat(factory.processorClientBuilder("processor-queue")).isNotNull();
    }
}
