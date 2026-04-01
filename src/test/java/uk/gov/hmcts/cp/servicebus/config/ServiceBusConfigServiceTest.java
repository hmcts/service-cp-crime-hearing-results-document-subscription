package uk.gov.hmcts.cp.servicebus.config;

import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClient;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceBusConfigServiceTest {

    private static final String EMULATOR_ADMIN_CONNECTION =
            "Endpoint=sb://localhost:5300;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=SAS_KEY_VALUE;UseDevelopmentEmulator=true;";
    private static final String EMULATOR_CONNECTION =
            "Endpoint=sb://localhost;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=SAS_KEY_VALUE;UseDevelopmentEmulator=true;";
    private static final String HTTPS_ENDPOINT = "https://test.servicebus.windows.net";
    private static final UUID CLIENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void admin_client_uses_local_emulator_when_disabled() {
        final ServiceBusConfigService service = new ServiceBusConfigService(
                false, EMULATOR_ADMIN_CONNECTION, EMULATOR_CONNECTION, 5, CLIENT_ID);

        assertThat(service.isEnabled()).isFalse();
        final ServiceBusAdministrationClient client = service.adminClient();
        assertThat(client).isNotNull();
    }

    @Test
    void admin_client_uses_https_when_enabled() {
        final ServiceBusConfigService service = new ServiceBusConfigService(
                true, HTTPS_ENDPOINT, EMULATOR_CONNECTION, 5, CLIENT_ID);

        assertThat(service.isEnabled()).isTrue();
        final ServiceBusAdministrationClient client = service.adminClient();
        assertThat(client).isNotNull();
    }

    @Test
    void enabled_true_defaults_to_real_service_bus() {
        final ServiceBusConfigService service = new ServiceBusConfigService(
                true, HTTPS_ENDPOINT, EMULATOR_CONNECTION, 5, CLIENT_ID);

        assertThat(service.isEnabled()).isTrue();
    }

    @Test
    void enabled_false_defaults_to_local_emulator() {
        final ServiceBusConfigService service = new ServiceBusConfigService(
                false, EMULATOR_ADMIN_CONNECTION, EMULATOR_CONNECTION, 5, CLIENT_ID);

        assertThat(service.isEnabled()).isFalse();
    }
}