package uk.gov.hmcts.cp.servicebus.config;

import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.cp.servicebus.admin.ServiceBusAdminAzureImpl;
import uk.gov.hmcts.cp.servicebus.admin.ServiceBusAdminBase;
import uk.gov.hmcts.cp.servicebus.admin.ServiceBusAdminEmulatorImpl;
import uk.gov.hmcts.cp.servicebus.admin.ServiceBusAdminInterface;
import uk.gov.hmcts.cp.vault.VaultServiceProperties;

@Slf4j
@Configuration
@AllArgsConstructor
public class ServiceBusAdminConfiguration {

    private final ServiceBusProperties configService;
    private final VaultServiceProperties vaultServiceProperties;

    @Bean
    public ServiceBusAdminInterface serviceBusAdminClient() {
        log.info("ServiceBusAdminConfiguration building serviceBusAdminClient isEmulator:{}", configService.isEmulator());
        if (configService.isEmulator()) {
            return new ServiceBusAdminEmulatorImpl(configService);
        } else {
            return new ServiceBusAdminAzureImpl(configService, vaultServiceProperties);
        }
    }

    @Bean
    public ServiceBusAdministrationClient administrationClient(final ServiceBusAdminInterface serviceBusAdminClient) {
        return ((ServiceBusAdminBase) serviceBusAdminClient).getAdminClient();
    }
}
