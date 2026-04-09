package uk.gov.hmcts.cp.subscription.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.cp.hmac.managers.HmacManager;
import uk.gov.hmcts.cp.hmac.model.KeyPair;
import uk.gov.hmcts.cp.openapi.model.ClientSubscription;
import uk.gov.hmcts.cp.openapi.model.ClientSubscriptionRequest;
import uk.gov.hmcts.cp.subscription.entities.ClientEntity;
import uk.gov.hmcts.cp.subscription.entities.ClientEventEntity;
import uk.gov.hmcts.cp.subscription.entities.ClientHmacEntity;
import uk.gov.hmcts.cp.subscription.mappers.ClientEntityMapper;
import uk.gov.hmcts.cp.subscription.mappers.ClientEventEntityMapper;
import uk.gov.hmcts.cp.subscription.mappers.ClientHmacMapper;
import uk.gov.hmcts.cp.subscription.mappers.ClientSubscriptionMapper;
import uk.gov.hmcts.cp.subscription.repositories.ClientEventRepository;
import uk.gov.hmcts.cp.subscription.repositories.ClientHmacRepository;
import uk.gov.hmcts.cp.subscription.repositories.ClientRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final ClientRepository clientRepository;
    private final ClientHmacMapper clientHmacMapper;
    private final ClientHmacRepository clientHmacRepository;
    private final ClientEventRepository clientEventRepository;

    private final ClientEntityMapper clientEntityMapper;
    private final ClientEventEntityMapper clientEventEntityMapper;
    private final ClientSubscriptionMapper clientSubscriptionMapper;

    private final ClockService clockService;
    private final HmacManager hmacManager;

    @Transactional
    public ClientSubscription createClientSubscription(final ClientSubscriptionRequest request,
                                                       final UUID clientId,
                                                       final List<Long> eventIds) {
        log.info("createClientSubscription clientId:{} eventTypeCount:{}", clientId, request.getEventTypes().size());
        final KeyPair keyPair = hmacManager.createAndStoreNewKey();

        final ClientEntity client = saveClientForCreateRequest(request, clientId);
        saveClientEvents(client.getSubscriptionId(), eventIds);
        saveClientHmac(client.getSubscriptionId(), keyPair.getKeyId());

        final ClientSubscription result = clientSubscriptionMapper.toDto(client, request.getEventTypes(), keyPair);
        log.info("createClientSubscription complete clientId:{} subscriptionId:{}", clientId, result.getClientSubscriptionId());
        return result;
    }

    @Transactional
    public ClientSubscription updateClientSubscription(final ClientSubscriptionRequest request,
                                                       final ClientEntity client,
                                                       final List<Long> eventIds) {
        log.info("updateClientSubscription clientId:{} subscriptionId:{}", client.getClientId(), client.getSubscriptionId());
        final ClientEntity updatedClient = saveClientForUpdateRequest(request, client);
        updateClientEvents(client.getSubscriptionId(), eventIds);

        return clientSubscriptionMapper.toDto(updatedClient, request.getEventTypes(), null);
    }

    public ClientSubscription getClientSubscription(final ClientEntity client) {
        log.info("getClientSubscription clientId:{} subscriptionId:{}", client.getClientId(), client.getSubscriptionId());
        final List<String> eventNames = clientEventRepository.findEventNamesForSubscription(client.getSubscriptionId());
        return clientSubscriptionMapper.toDto(client, eventNames, null);
    }

    @Transactional
    public void deleteClientSubscription(final ClientEntity client) {
        log.info("deleteClientSubscription clientId:{} subscriptionId:{}", client.getClientId(), client.getSubscriptionId());
        clientHmacRepository.deleteAllBySubscriptionId(client.getSubscriptionId());
        clientEventRepository.deleteBySubscriptionId(client.getSubscriptionId());
        clientRepository.delete(client);
    }

    public boolean hasAccess(final UUID subscriptionId,
                             final String eventType) {
        return clientEventRepository.countByClientSubscriptionAndEventName(subscriptionId, eventType) > 0;
    }

    private void saveClientEvents(final UUID subscriptionId, final List<Long> eventIds) {
        final List<ClientEventEntity> eventEntities = eventIds.stream()
                .map(id -> clientEventEntityMapper.toEntity(subscriptionId, id))
                .toList();
        clientEventRepository.saveAll(eventEntities);
    }

    private void saveClientHmac(final UUID subscriptionId, final String hmacKeyId) {
        final ClientHmacEntity clientHmacEntity = clientHmacMapper.toEntity(subscriptionId, hmacKeyId);
        clientHmacRepository.save(clientHmacEntity);
    }

    private ClientEntity saveClientForCreateRequest(final ClientSubscriptionRequest request, final UUID clientId) {
        final ClientEntity client = clientEntityMapper.toEntity(clockService, request, clientId);
        return clientRepository.save(client);
    }

    private ClientEntity saveClientForUpdateRequest(final ClientSubscriptionRequest request, final ClientEntity client) {
        final ClientEntity updatedClient = clientEntityMapper.mapUpdateRequestToEntity(client, clockService, request);
        return clientRepository.save(updatedClient);
    }

    private void updateClientEvents(final UUID subscriptionId, final List<Long> eventIds) {
        clientEventRepository.deleteBySubscriptionId(subscriptionId);
        saveClientEvents(subscriptionId, eventIds);
    }
}