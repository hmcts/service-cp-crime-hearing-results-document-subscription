package uk.gov.hmcts.cp.subscription.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
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

    @Transactional
    public ClientSubscription saveSubscription(final ClientSubscriptionRequest request, final String clientId) {
        ClientSubscriptionEntity entity = mapper.mapCreateRequestToEntity(clockService, request);
        entity = entity.toBuilder().clientId(clientId).build();
        return mapper.mapEntityToResponse(clockService, subscriptionRepository.save(entity));
    }

    @Transactional
    public ClientSubscription updateSubscription(final UUID clientSubscriptionId, final ClientSubscriptionRequest request, final String clientId) {
        final ClientSubscriptionEntity existing = subscriptionRepository.findByIdAndClientId(clientSubscriptionId, clientId)
                .orElseThrow(() -> new EntityNotFoundException("Subscription not found"));
        final ClientSubscriptionEntity entity = mapper.mapUpdateRequestToEntity(clockService, existing, request);
        return mapper.mapEntityToResponse(clockService, subscriptionRepository.save(entity));
    }

    @Transactional
    public ClientSubscription getSubscription(final UUID clientSubscriptionId, final String clientId) {
        final ClientSubscriptionEntity entity = subscriptionRepository.findByIdAndClientId(clientSubscriptionId, clientId)
                .orElseThrow(() -> new EntityNotFoundException("Subscription not found"));
        return mapper.mapEntityToResponse(clockService, entity);
    }

    @Transactional
    public void deleteSubscription(final UUID clientSubscriptionId, final String clientId) {
        final ClientSubscriptionEntity entity = subscriptionRepository.findByIdAndClientId(clientSubscriptionId, clientId)
                .orElseThrow(() -> new EntityNotFoundException("Subscription not found"));
        subscriptionRepository.delete(entity);
    }

    @Transactional
    public boolean hasAccess(final UUID clientSubscriptionId, final String clientId, final EntityEventType eventType) {
        return subscriptionRepository.findByIdAndClientId(clientSubscriptionId, clientId)
                .map(entity -> entity.getEventTypes() != null && entity.getEventTypes().contains(eventType))
                .orElse(false);
    }
}
