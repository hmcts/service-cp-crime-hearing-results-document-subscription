package uk.gov.hmcts.cp;

import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClient;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClientBuilder;
import com.azure.messaging.servicebus.administration.models.CreateQueueOptions;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.servicebus.config.ServiceBusConfigService;
import uk.gov.hmcts.cp.servicebus.services.ServiceBusAdminService;
import uk.gov.hmcts.cp.subscription.repositories.EventTypeRepository;

import java.time.Duration;

/**
 * We add any debug logging we might need such as database checks
 */
@Slf4j
@Service
@AllArgsConstructor
public class PostStartup {
    private EventTypeRepository eventTypeRepository;
    private ServiceBusConfigService configService;
    private ServiceBusAdminService adminService;

    @PostConstruct
    public void postStartupLogging() {
        log.info("PostStartup Database contains {} eventTypes", eventTypeRepository.count());
        logServiceBusStatus();
    }

    private void logServiceBusStatus() {
        try {
            log.info("PostStartup service bus getting credentials");
            final DefaultAzureCredential credential = new DefaultAzureCredentialBuilder()
                    .build();
            log.info("PostStartup service bus connecting for admin client");

            // BUG: Do not use both connectionString() and credential() on the same builder.
            // A connection string already contains a SAS key and is self-sufficient for auth.
            // Adding DefaultAzureCredential on top forces the Azure SDK to attempt token-based
            // authentication via its credential chain (env vars → managed identity → Azure CLI → etc.).
            // In pipelines or environments without a managed identity attached, the managed identity
            // probe issues HTTP requests that have long timeouts and no fast-fail — this is why the
            // pipeline hangs.
            //
            // FIX: Use one auth mechanism, not both:
            //   - Pipeline / local / DEV  → .connectionString(...) only, remove .credential(...)
            //   - Production Azure (MI)   → .fullyQualifiedNamespace(...) + .credential(...), no connectionString
            //
            // Also check ServiceBusAdminService for the same pattern.
            new ServiceBusAdministrationClientBuilder()
                    .connectionString(configService.getConnectionString())
                    .credential(credential)
                    .buildClient();
            log.info("SKIPPING PostStartup service bus listing queues");
        } catch (Exception e) {
            log.error("PostStartup service bus error.", e);
        }
    }

    private void createQueue(final ServiceBusAdministrationClient adminClient, final String queueName) {
        if (adminClient.getQueueExists(queueName)) {
            log.info("Queue {} already exists", queueName);
        } else {
            log.info("Creating queue {}", queueName);
            final CreateQueueOptions createQueueOptions = new CreateQueueOptions();
            createQueueOptions.setDefaultMessageTimeToLive(Duration.ofHours(1));
            createQueueOptions.setMaxDeliveryCount(1);
            adminClient.createQueue(queueName, createQueueOptions);
        }
    }
}
