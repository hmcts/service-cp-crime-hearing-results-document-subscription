package uk.gov.hmcts.cp;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.subscription.repositories.EventTypeRepository;

/**
 * We add any debug logging we might need such as database checks
 */
@Slf4j
@Service
@AllArgsConstructor
public class PostStartup {
    EventTypeRepository eventTypeRepository;

    @PostConstruct
    void postStartupLogging() {
        log.info("Database contains {} eventTypes", eventTypeRepository.count());
    }
}
