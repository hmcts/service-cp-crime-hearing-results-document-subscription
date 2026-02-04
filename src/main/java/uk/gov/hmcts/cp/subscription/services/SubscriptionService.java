package uk.gov.hmcts.cp.subscription.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uk.gov.hmcts.cp.openapi.model.ClientSubscription;
import uk.gov.hmcts.cp.openapi.model.ClientSubscriptionRequest;
import uk.gov.hmcts.cp.openapi.model.CreateClientSubscriptionRequest;
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
    public ClientSubscription saveSubscription(final String callbackUrl, final CreateClientSubscriptionRequest request) {
        final ClientSubscriptionEntity entity = mapper.mapCreateRequestToEntity(clockService, callbackUrl, request);
        return mapper.mapEntityToResponse(clockService, subscriptionRepository.save(entity));
    }

    @Transactional
    public ClientSubscription updateSubscription(final UUID clientSubscriptionId, final ClientSubscriptionRequest request) {
        final ClientSubscriptionEntity existing = subscriptionRepository.getReferenceById(clientSubscriptionId);
        final ClientSubscriptionEntity entity = mapper.mapUpdateRequestToEntity(clockService, existing, request);
        return mapper.mapEntityToResponse(clockService, subscriptionRepository.save(entity));
    }

    @Transactional
    public ClientSubscription getSubscription(final UUID clientSubscriptionId) {
        final ClientSubscriptionEntity entity = subscriptionRepository.getReferenceById(clientSubscriptionId);
        return mapper.mapEntityToResponse(clockService, entity);
    }

    @Transactional
    public void deleteSubscription(final UUID clientSubscriptionId) {
        subscriptionRepository.deleteById(clientSubscriptionId);
    }

    @Transactional
    public boolean hasAccess(final UUID clientSubscriptionId, final EntityEventType eventType) {
        return subscriptionRepository.existsById(clientSubscriptionId)
                && subscriptionRepository.existsByIdAndEventType(clientSubscriptionId, eventType.name());
    }
}
