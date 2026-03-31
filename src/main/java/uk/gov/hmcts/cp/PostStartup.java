package uk.gov.hmcts.cp;

import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClientBuilder;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.servicebus.config.ServiceBusConfigService;
import uk.gov.hmcts.cp.subscription.repositories.EventTypeRepository;

/**
 * We add any debug logging we might need such as database checks
 */
@Slf4j
@Service
@AllArgsConstructor
public class PostStartup {
    private EventTypeRepository eventTypeRepository;
    private ServiceBusConfigService configService;

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

}
