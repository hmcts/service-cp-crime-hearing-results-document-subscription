package uk.gov.hmcts.cp.subscription.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.openapi.model.EventNotificationPayload;
import uk.gov.hmcts.cp.openapi.model.PcrEventPayload;
import uk.gov.hmcts.cp.servicebus.config.ServiceBusConfigService;
import uk.gov.hmcts.cp.servicebus.services.ServiceBusService;
import uk.gov.hmcts.cp.subscription.entities.ClientSubscriptionEntity;
import uk.gov.hmcts.cp.subscription.mappers.NotificationMapper;
import uk.gov.hmcts.cp.subscription.mappers.SubscriberMapper;
import uk.gov.hmcts.cp.subscription.model.EntityEventType;
import uk.gov.hmcts.cp.subscription.model.Subscriber;
import uk.gov.hmcts.cp.subscription.repositories.SubscriptionRepository;

import java.util.List;
import java.util.UUID;

import static uk.gov.hmcts.cp.servicebus.config.ServiceBusConfigService.TOPIC_NAME;

@Service
@RequiredArgsConstructor
@Slf4j
public class CallbackDeliveryService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriberMapper subscriberMapper;
    private final NotificationMapper notificationMapper;
    private final JsonMapper jsonMapper;
    private final ServiceBusConfigService serviceBusConfig;
    private final ServiceBusService serviceBusService;
    private final CallbackService callbackService;

    public void processPcrEvent(final PcrEventPayload pcrEventPayload, final UUID documentId) {
        final EntityEventType eventType = EntityEventType.valueOf(pcrEventPayload.getEventType().name());
        final List<ClientSubscriptionEntity> entities = subscriptionRepository.findByEventType(eventType.name());
        final EventNotificationPayload eventNotificationPayload = notificationMapper.mapToPayload(documentId, pcrEventPayload);
        for (final ClientSubscriptionEntity entity : entities) {
            final Subscriber subscriber = subscriberMapper.toSubscriber(entity);
            if (serviceBusConfig.isEnabled()) {
                final String payload = jsonMapper.toJson(eventNotificationPayload);
                serviceBusService.queueMessage(TOPIC_NAME, subscriber.getNotificationEndpoint(), payload, 0);
            } else {
                callbackService.sendToSubscriber(subscriber.getNotificationEndpoint(), eventNotificationPayload);
                log.info("Subscriber {} notified via callbackUrl {} for documentId {}", subscriber.getId(), subscriber.getNotificationEndpoint(), eventNotificationPayload.getDocumentId());
            }
        }
    }
}
