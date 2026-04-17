package uk.gov.hmcts.cp;

import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClient;
import com.azure.messaging.servicebus.administration.models.QueueRuntimeProperties;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.servicebus.config.ServiceBusProperties;
import uk.gov.hmcts.cp.subscription.config.AppProperties;
import uk.gov.hmcts.cp.subscription.repositories.DocumentMappingRepository;
import uk.gov.hmcts.cp.subscription.repositories.EventTypeRepository;

import java.time.OffsetDateTime;

/**
 * We can add any debug logging we might need such as database checks
 */
@Slf4j
@Service
@AllArgsConstructor
public class PostStartup {
    private AppProperties appProperties;
    private EventTypeRepository eventTypeRepository;
    private DocumentMappingRepository documentMappingRepository;
    private ServiceBusAdministrationClient administrationClient;

    @PostConstruct
    public void postStartupLogging() {
        log.info("PostStartup Database contains {} eventTypes", eventTypeRepository.count());
        logRecentDocumentMappings();
        logDeadLetterQueueSizes();
    }

    private void logDeadLetterQueueSizes() {
        logDeadLetterCount(ServiceBusProperties.NOTIFICATIONS_INBOUND_QUEUE);
        logDeadLetterCount(ServiceBusProperties.NOTIFICATIONS_OUTBOUND_QUEUE);
    }

    private void logDeadLetterCount(final String queueName) {
        try {
            final QueueRuntimeProperties runtimeProperties = administrationClient.getQueueRuntimeProperties(queueName);
            log.info("PostStartup Queue {} deadLetterMessageCount:{} activeMessageCount:{}",
                    queueName, runtimeProperties.getDeadLetterMessageCount(), runtimeProperties.getActiveMessageCount());
        } catch (Exception e) {
            log.warn("PostStartup Failed to get dead letter count for queue {}: {}", queueName, e.getMessage());
        }
    }

    private void logRecentDocumentMappings() {
        log.info("PostStartup Database contains {} document mappings", documentMappingRepository.count());
        final OffsetDateTime lastMonthDate = OffsetDateTime.now().minusMonths(1);
        documentMappingRepository.findAll().stream().filter(d -> d.getCreatedAt().isAfter(lastMonthDate)).forEach(
                d -> log.info("PostStartup Database contains documentId:{} materialId:{} createdAt:{}", d.getDocumentId(), d.getMaterialId(), d.getCreatedAt())
        );
    }
}
