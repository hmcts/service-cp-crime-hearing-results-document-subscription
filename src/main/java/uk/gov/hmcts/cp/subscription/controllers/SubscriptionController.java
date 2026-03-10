package uk.gov.hmcts.cp.subscription.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.cp.filters.ClientIdResolutionFilter;
import uk.gov.hmcts.cp.openapi.api.SubscriptionApi;
import uk.gov.hmcts.cp.openapi.model.ClientSubscription;
import uk.gov.hmcts.cp.openapi.model.ClientSubscriptionRequest;
import uk.gov.hmcts.cp.subscription.services.SubscriptionService;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class SubscriptionController implements SubscriptionApi {

    private final SubscriptionService subscriptionService;

    @Override
    public ResponseEntity<ClientSubscription> createClientSubscription(final ClientSubscriptionRequest request) {
        final UUID clientId = UUID.fromString(MDC.get(ClientIdResolutionFilter.MDC_CLIENT_ID));
        // log.info("createClientSubscription callbackUrl:{} clientId:{}",
        //         Encode.forJava(request.getNotificationEndpoint().getCallbackUrl()), clientId);
        log.info("createClientSubscription callbackUrl:{} clientId:{}", request.getNotificationEndpoint().getCallbackUrl(), clientId);
        final ClientSubscription response = subscriptionService.saveSubscription(request, clientId);
        log.info("createClientSubscription created subscription:{}", response.getClientSubscriptionId());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<ClientSubscription> updateClientSubscription(final UUID clientSubscriptionId,
                                                                       final ClientSubscriptionRequest request) {
        final UUID clientId = UUID.fromString(MDC.get(ClientIdResolutionFilter.MDC_CLIENT_ID));
        log.info("updateClientSubscription clientSubscriptionId:{} clientId:{}", clientSubscriptionId, clientId);
        final ClientSubscription response = subscriptionService.updateSubscription(clientSubscriptionId, request, clientId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<ClientSubscription> getClientSubscription(final UUID clientSubscriptionId) {
        final UUID clientId = UUID.fromString(MDC.get(ClientIdResolutionFilter.MDC_CLIENT_ID));
        log.info("getClientSubscription clientSubscriptionId:{} clientId:{}", clientSubscriptionId, clientId);
        final ClientSubscription response = subscriptionService.getSubscription(clientSubscriptionId, clientId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> deleteClientSubscription(final UUID clientSubscriptionId) {
        final UUID clientId = UUID.fromString(MDC.get(ClientIdResolutionFilter.MDC_CLIENT_ID));
        log.info("deleteClientSubscription clientSubscriptionId:{} clientId:{}", clientSubscriptionId, clientId);
        subscriptionService.deleteSubscription(clientSubscriptionId, clientId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
