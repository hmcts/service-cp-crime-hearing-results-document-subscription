package uk.gov.hmcts.cp.servicebus.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.openapi.model.EventNotificationPayload;
import uk.gov.hmcts.cp.openapi.model.EventPayload;
import uk.gov.hmcts.cp.subscription.clients.CallbackClient;
import uk.gov.hmcts.cp.subscription.managers.NotificationManager;
import uk.gov.hmcts.cp.subscription.services.JsonMapper;

import static uk.gov.hmcts.cp.servicebus.config.ServiceBusConfigService.PCR_INBOUND_TOPIC;
import static uk.gov.hmcts.cp.servicebus.config.ServiceBusConfigService.PCR_OUTBOUND_TOPIC;

@Service
@AllArgsConstructor
@Slf4j
public class ServiceBusHandlers {

    private final CallbackClient callbackClient;
    private final NotificationManager notificationManager;
    private final JsonMapper jsonMapper;

    public void handleMessage(final String topicName, final String target, final String message) {
        switch (topicName) {
            case PCR_INBOUND_TOPIC -> {
                final EventPayload eventPayload = jsonMapper.fromJson(message, EventPayload.class);
                log.info("handleMessageType {} eventId:{}", topicName, eventPayload.getEventId());
                notificationManager.processPcrNotification(eventPayload);
            }
            case PCR_OUTBOUND_TOPIC -> {
                final EventNotificationPayload eventNotificationPayload = jsonMapper.fromJson(message, EventNotificationPayload.class);
                log.info("handleMessageType {} documentId:{}", topicName, eventNotificationPayload.getDocumentId());
                callbackClient.sendNotification(target, eventNotificationPayload);
            }
            default -> throw new RuntimeException("Invalid topic name " + topicName);
        }
        log.info("handleMessageType completed OK");
    }
}
