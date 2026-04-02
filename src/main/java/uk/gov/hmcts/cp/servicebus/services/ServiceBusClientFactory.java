package uk.gov.hmcts.cp.servicebus.services;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.cp.servicebus.config.ServiceBusProperties;
import uk.gov.hmcts.cp.vault.VaultServiceProperties;

import java.net.URI;

@Component
@AllArgsConstructor
public class ServiceBusClientFactory {

    private final ServiceBusProperties properties;
    private final VaultServiceProperties vaultServiceProperties;

    public ServiceBusSenderClient senderClient(final String queueName) {
        return clientBuilder().sender().queueName(queueName).buildClient();
    }

    public ServiceBusClientBuilder.ServiceBusProcessorClientBuilder processorClientBuilder(final String queueName) {
        return clientBuilder().processor().queueName(queueName);
    }

    private ServiceBusClientBuilder clientBuilder() {
        if (properties.isEmulator()) {
            return new ServiceBusClientBuilder().connectionString(properties.getConnectionString());
        }
        final String namespace = URI.create(properties.getConnectionString()).getHost();
        return new ServiceBusClientBuilder()
                .fullyQualifiedNamespace(namespace)
                .credential(new DefaultAzureCredentialBuilder()
                        .managedIdentityClientId(vaultServiceProperties.getVaultClientId().toString())
                        .build());
    }
}