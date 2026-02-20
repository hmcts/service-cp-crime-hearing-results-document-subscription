package uk.gov.hmcts.cp.subscription.controllers;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.cp.subscription.filter.ClientIdResolutionFilter;
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
    private final HttpServletRequest httpRequest;

    @Override
    public ResponseEntity<ClientSubscription> createClientSubscription(final ClientSubscriptionRequest request) {
        log.info("createClientSubscription callbackUrl:{} clientId:{}",
                Encode.forJava(request.getNotificationEndpoint().getCallbackUrl()), ClientIdResolutionFilter.getResolvedClientId(httpRequest));
        final ClientSubscription response = subscriptionService.saveSubscription(request, ClientIdResolutionFilter.getResolvedClientId(httpRequest));
        log.info("createClientSubscription created subscription:{}", response.getClientSubscriptionId());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<ClientSubscription> updateClientSubscription(final UUID clientSubscriptionId,
                                                                       final ClientSubscriptionRequest request) {
        log.info("updateClientSubscription clientSubscriptionId:{} clientId:{}", clientSubscriptionId, ClientIdResolutionFilter.getResolvedClientId(httpRequest));
        final ClientSubscription response = subscriptionService.updateSubscription(clientSubscriptionId, request, ClientIdResolutionFilter.getResolvedClientId(httpRequest));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<ClientSubscription> getClientSubscription(final UUID clientSubscriptionId) {
        log.info("getClientSubscription clientSubscriptionId:{} clientId:{}", clientSubscriptionId, ClientIdResolutionFilter.getResolvedClientId(httpRequest));
        final ClientSubscription response = subscriptionService.getSubscription(clientSubscriptionId, ClientIdResolutionFilter.getResolvedClientId(httpRequest));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> deleteClientSubscription(final UUID clientSubscriptionId) {
        log.info("deleteClientSubscription clientSubscriptionId:{} clientId:{}", clientSubscriptionId, ClientIdResolutionFilter.getResolvedClientId(httpRequest));
        subscriptionService.deleteSubscription(clientSubscriptionId, ClientIdResolutionFilter.getResolvedClientId(httpRequest));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
