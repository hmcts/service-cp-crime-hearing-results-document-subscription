package uk.gov.hmcts.cp.servicebus.admin;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClientBuilder;
import uk.gov.hmcts.cp.servicebus.config.ServiceBusProperties;
import uk.gov.hmcts.cp.vault.VaultServiceProperties;

public class ServiceBusAdminAzureImpl extends ServiceBusAdminBase {

    public ServiceBusAdminAzureImpl(final ServiceBusProperties properties,
                                    final VaultServiceProperties vaultServiceProperties) {
        super(new ServiceBusAdministrationClientBuilder()
                .endpoint(properties.getConnectionString())
                .credential(new DefaultAzureCredentialBuilder()
                        .managedIdentityClientId(vaultServiceProperties.getVaultClientId().toString())
                        .build())
                .buildClient());
    }
}
