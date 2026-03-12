package uk.gov.hmcts.cp.subscription.services;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.cp.hmac.services.HmacKeyService;
import uk.gov.hmcts.cp.hmac.services.HmacKeyStore;
import uk.gov.hmcts.cp.hmac.services.HmacKeyStoreService;
import uk.gov.hmcts.cp.openapi.model.ClientSubscription;
import uk.gov.hmcts.cp.openapi.model.ClientSubscriptionRequest;
import uk.gov.hmcts.cp.subscription.entities.ClientSubscriptionEntity;
import uk.gov.hmcts.cp.subscription.mappers.SubscriptionMapper;
import uk.gov.hmcts.cp.subscription.model.EntityEventType;
import uk.gov.hmcts.cp.subscription.repositories.SubscriptionRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final ClockService clockService;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionMapper mapper;
    private final HmacKeyStore hmacKeyStore;

    @Transactional
    public ClientSubscription saveSubscription(final ClientSubscriptionRequest request, final UUID clientId) {
        subscriptionRepository.findFirstByClientId(clientId).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "subscription already exist with " + existing.getId());
        });
        ClientSubscriptionEntity entity = mapper.mapCreateRequestToEntity(request, clockService.nowOffsetUTC());
        entity = entity.toBuilder().clientId(clientId).build();
        entity = subscriptionRepository.save(entity);

        final ClientSubscription response = mapper.mapEntityToResponse(entity);
        final HmacKeyService.KeyPair keyPair = hmacKeyStore.generateAndStore(entity.getId());
        response.setKeyId(keyPair.keyId());
        response.setSecret(keyPair.secret());
        return response;
    }

    @Transactional
    public ClientSubscription updateSubscription(final UUID clientSubscriptionId, final ClientSubscriptionRequest request, final UUID clientId) {
        final ClientSubscriptionEntity existing = subscriptionRepository.findByIdAndClientId(clientSubscriptionId, clientId)
                .orElseThrow(() -> new EntityNotFoundException("Subscription not found"));
        final ClientSubscriptionEntity entity = mapper.mapUpdateRequestToEntity(existing, request, clockService.nowOffsetUTC());
        return mapper.mapEntityToResponse(subscriptionRepository.save(entity));
    }

    @Transactional
    public ClientSubscription getSubscription(final UUID clientSubscriptionId, final UUID clientId) {
        final ClientSubscriptionEntity entity = subscriptionRepository.findByIdAndClientId(clientSubscriptionId, clientId)
                .orElseThrow(() -> new EntityNotFoundException("Subscription not found"));
        return mapper.mapEntityToResponse(entity);
    }

    @Transactional
    public void deleteSubscription(final UUID clientSubscriptionId, final UUID clientId) {
        final ClientSubscriptionEntity entity = subscriptionRepository.findByIdAndClientId(clientSubscriptionId, clientId)
                .orElseThrow(() -> new EntityNotFoundException("Subscription not found"));
        subscriptionRepository.delete(entity);
    }

    @Transactional
    public boolean hasAccess(final UUID clientSubscriptionId, final UUID clientId, final EntityEventType eventType) {
        return subscriptionRepository.findByIdAndClientId(clientSubscriptionId, clientId)
                .map(entity -> entity.getEventTypes() != null && entity.getEventTypes().contains(eventType))
                .orElse(false);
    }
}
