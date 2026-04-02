package uk.gov.hmcts.cp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cp.subscription.repositories.EventTypeRepository;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostStartupTest {

    @Mock
    EventTypeRepository eventTypeRepository;

    @InjectMocks
    PostStartup postStartup;

    @Test
    void post_startup_should_log_event_type_count() {
        postStartup.postStartupLogging();
        verify(eventTypeRepository).count();
    }
}