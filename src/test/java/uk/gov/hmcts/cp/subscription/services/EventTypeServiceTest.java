package uk.gov.hmcts.cp.subscription.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cp.openapi.model.EventTypeResponse;
import uk.gov.hmcts.cp.subscription.entities.EventTypeEntity;
import uk.gov.hmcts.cp.subscription.mappers.EventTypeMapper;
import uk.gov.hmcts.cp.subscription.repositories.EventTypeRepository;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventTypeServiceTest {

    @Mock
    private EventTypeRepository eventTypeRepository;

    @Mock
    private EventTypeMapper eventTypeMapper;

    @InjectMocks
    private EventTypeService eventTypeService;

    @Test
    void all_event_types_should_be_returned() {
        EventTypeEntity entity1 = new EventTypeEntity(1L, "PRISON_COURT_REGISTER_GENERATED", "Prison court register", "REGISTER");
        EventTypeEntity entity2 = new EventTypeEntity(2L, "WEE_Layout5", "Warrant Supplement", "WARRANT");
        List<EventTypeEntity> entityList = List.of(entity1, entity2);

        EventTypeResponse eventTypes = Mockito.mock(EventTypeResponse.class);

        when(eventTypeRepository.findAll()).thenReturn(entityList);
        when(eventTypeMapper.mapToEventTypes(entityList)).thenReturn(eventTypes);

        EventTypeResponse response = eventTypeService.getAllEventTypes();
        assertThat(response).isEqualTo(eventTypes);

        verify(eventTypeRepository).findAll();
        verify(eventTypeMapper).mapToEventTypes(entityList);

    }
}