package uk.gov.hmcts.cp;

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
            log.info("PostStartup service bus connecting for admin client");
            // Using connectionString only — it contains the SAS key and is self-sufficient.
            // Do not add .credential() here: mixing connectionString with DefaultAzureCredential
            // causes the SDK to probe managed identity endpoints which hang in pipelines.
            new ServiceBusAdministrationClientBuilder()
                    .connectionString(configService.getConnectionString())
                    .buildClient();
            log.info("PostStartup service bus admin client connected successfully");
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
