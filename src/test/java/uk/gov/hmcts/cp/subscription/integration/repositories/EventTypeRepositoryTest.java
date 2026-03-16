package uk.gov.hmcts.cp.subscription.integration.repositories;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.cp.subscription.entities.EventTypeEntity;
import uk.gov.hmcts.cp.subscription.integration.IntegrationTestBase;

import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class EventTypeRepositoryTest extends IntegrationTestBase {

    @Transactional
    @Test
    void findAllEventTypes_should_return_document() {
        EventTypeEntity entity1 = EventTypeEntity.builder()
                .id(1L)
                .eventName("PRISON_COURT_REGISTER_GENERATED")
                .displayName("Prison court register")
                .category("REGISTER")
                .build();
        EventTypeEntity entity2 = EventTypeEntity.builder()
                .id(2L)
                .eventName("WEE_Layout5")
                .displayName("Warrant Supplement")
                .category("WARRANT")
                .build();

        List<EventTypeEntity> found = eventTypeRepository.findAll();
        assertThat(found.size()).isEqualTo(41);
        assertThat(found).contains(entity1, entity2);
    }
}
