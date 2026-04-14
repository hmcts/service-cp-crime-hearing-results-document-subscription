package uk.gov.hmcts.cp;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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

    @PostConstruct
    public void postStartupLogging() {
        log.info("PostStartup Database contains {} eventTypes", eventTypeRepository.count());
        logRecentDocumentMappings();
    }

    private void logRecentDocumentMappings() {
        log.info("PostStartup Database contains {} document mappings", documentMappingRepository.count());
        final OffsetDateTime lastMonthDate = OffsetDateTime.now().minusMonths(1);
        documentMappingRepository.findAll().stream().filter(d -> d.getCreatedAt().isAfter(lastMonthDate)).forEach(
                d -> log.info("PostStartup Database contains documentId:{} materialId:{} createdAt:{}", d.getDocumentId(), d.getMaterialId(), d.getCreatedAt())
        );
    }
}
