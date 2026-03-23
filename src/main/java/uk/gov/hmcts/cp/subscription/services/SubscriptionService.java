package uk.gov.hmcts.cp.subscription.services;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.cp.hmac.model.KeyPair;
import uk.gov.hmcts.cp.hmac.services.HmacKeyService;
import uk.gov.hmcts.cp.openapi.model.ClientSubscription;
import uk.gov.hmcts.cp.openapi.model.ClientSubscriptionRequest;
import uk.gov.hmcts.cp.subscription.entities.ClientSubscriptionEntity;
import uk.gov.hmcts.cp.subscription.mappers.SubscriptionMapper;
import uk.gov.hmcts.cp.subscription.repositories.SubscriptionRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final ClockService clockService;
    private final SubscriptionRepository subscriptionRepository;
    private final ClientEventsService clientEventsService;
    private final SubscriptionMapper mapper;
    private final HmacKeyService hmacKeyService;

    @Transactional
    public ClientSubscription createSubscription(final ClientSubscriptionRequest request, final UUID clientId) {
        subscriptionRepository.findFirstByClientId(clientId).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "subscription already exist with " + existing.getId());
        });
        final ClientSubscriptionEntity entity = mapper.mapCreateRequestToEntity(clientId, request, clockService.nowOffsetUTC());
        subscriptionRepository.save(entity);
        final KeyPair keyPair = hmacKeyService.generateKey();
        final ClientSubscription response = mapper.mapEntityToResponse(entity, keyPair);
        clientEventsService.saveClientInfo(response, clientId);
        return response;
    }

    @Transactional
    public ClientSubscription updateSubscription(final UUID clientSubscriptionId, final ClientSubscriptionRequest request, final UUID clientId) {
        final ClientSubscriptionEntity existing = subscriptionRepository.findByIdAndClientId(clientSubscriptionId, clientId)
                .orElseThrow(() -> new EntityNotFoundException("Subscription not found"));
        final ClientSubscriptionEntity entity = mapper.mapUpdateRequestToEntity(existing, request, clockService.nowOffsetUTC());
        final ClientSubscription response = mapper.mapEntityToResponse(subscriptionRepository.save(entity), null);
        clientEventsService.updateClientInfo(response, clientId);
        return response;
    }

    @Transactional
    public ClientSubscription getSubscription(final UUID clientSubscriptionId, final UUID clientId) {
        final ClientSubscriptionEntity entity = subscriptionRepository.findByIdAndClientId(clientSubscriptionId, clientId)
                .orElseThrow(() -> new EntityNotFoundException("Subscription not found"));
        return mapper.mapEntityToResponse(entity, null);
    }

    @Transactional
    public void deleteSubscription(final UUID clientSubscriptionId, final UUID clientId) {
        final ClientSubscriptionEntity entity = subscriptionRepository.findByIdAndClientId(clientSubscriptionId, clientId)
                .orElseThrow(() -> new EntityNotFoundException("Subscription not found"));
        clientEventsService.deleteClientInfo(clientSubscriptionId, clientId);
        subscriptionRepository.delete(entity);
    }

}
