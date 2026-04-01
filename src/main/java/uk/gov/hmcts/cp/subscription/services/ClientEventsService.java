package uk.gov.hmcts.cp.subscription.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.openapi.model.ClientSubscription;
import uk.gov.hmcts.cp.subscription.entities.ClientEntity;
import uk.gov.hmcts.cp.subscription.entities.ClientEventEntity;
import uk.gov.hmcts.cp.subscription.entities.EventTypeEntity;
import uk.gov.hmcts.cp.subscription.mappers.ClientEventMapper;
import uk.gov.hmcts.cp.subscription.mappers.ClientMapper;
import uk.gov.hmcts.cp.subscription.repositories.ClientEventsRepository;
import uk.gov.hmcts.cp.subscription.repositories.ClientRepository;
import uk.gov.hmcts.cp.subscription.repositories.EventTypeRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientEventsService {

    private final ClientRepository clientRepository;
    private final ClientEventsRepository clientEventsRepository;
    private final EventTypeRepository eventTypeRepository;

    private final ClientMapper clientMapper;
    private final ClientEventMapper clientEventMapper;

    private final ClockService clockService;

    public void saveClientInfo(final ClientSubscription clientSubscription, final UUID clientId) {
        final ClientEntity clientEntity = clientMapper.mapToClientEntity(clockService, clientSubscription, clientId);
        clientRepository.save(clientEntity);

        final List<ClientEventEntity> clientEventEntityList = clientEventMapper.mapToClientEventEntityList(clientSubscription.getClientSubscriptionId(), getEventTypes(clientSubscription));
        clientEventsRepository.saveAll(clientEventEntityList);
    }

    /* package */
    Set<EventTypeEntity> getEventTypes(final ClientSubscription clientSubscription) {
        final List<EventTypeEntity> eventTypeEntities = eventTypeRepository.findAll();
        return clientSubscription.getEventTypes()
                .stream()
                .map(eventType -> eventTypeEntities.stream()
                        .filter(e -> e.getEventName().equals(eventType))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Invalid event type: " + eventType)))
                .collect(Collectors.toSet());
    }
}
