package uk.gov.hmcts.cp.subscription.mappers;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.cp.openapi.model.EventTypePayload;
import uk.gov.hmcts.cp.openapi.model.EventTypeResponse;
import uk.gov.hmcts.cp.subscription.entities.EventTypeEntity;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class EventTypeMapperTest {

    EventTypeMapper eventTypeMapper = new EventTypeMapperImpl();

    @Test
    void entity_should_map_to_payload() {
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

        EventTypePayload expectedEventType1 = EventTypePayload.builder()
                .eventName("PRISON_COURT_REGISTER_GENERATED")
                .displayName("Prison court register")
                .category("REGISTER")
                .build();
        EventTypePayload expectedEventType2 = EventTypePayload.builder()
                .eventName("WEE_Layout5")
                .displayName("Warrant Supplement")
                .category("WARRANT")
                .build();

        List<EventTypePayload> expectedEventTypes = List.of(expectedEventType1, expectedEventType2);
        EventTypeResponse eventTypes = eventTypeMapper.mapToEventTypes(List.of(entity1, entity2));
        assertThat(eventTypes.getEvents()).isEqualTo(expectedEventTypes);
    }


}