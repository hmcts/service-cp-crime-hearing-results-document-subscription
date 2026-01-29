package uk.gov.hmcts.cp.subscription.unit.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cp.openapi.model.ClientSubscription;
import uk.gov.hmcts.cp.openapi.model.ClientSubscriptionRequest;
import uk.gov.hmcts.cp.openapi.model.CreateClientSubscriptionRequest;
import uk.gov.hmcts.cp.subscription.controllers.SubscriptionController;
import uk.gov.hmcts.cp.subscription.services.SubscriptionService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionControllerTest {

    @Mock
    SubscriptionService subscriptionService;

    @InjectMocks
    SubscriptionController subscriptionController;

    ClientSubscriptionRequest request = ClientSubscriptionRequest.builder().build();
    CreateClientSubscriptionRequest createClientSubscriptionRequest = CreateClientSubscriptionRequest.builder().build();
    UUID subscriptionId = UUID.randomUUID();
    String callbackUrl = "https://example.com/callback";

    @Test
    void create_controller_should_call_service() {
        ClientSubscription response = ClientSubscription.builder().clientSubscriptionId(subscriptionId).build();
        when(subscriptionService.saveSubscription(callbackUrl, createClientSubscriptionRequest)).thenReturn(response);
        var result = subscriptionController.createClientSubscription(callbackUrl, createClientSubscriptionRequest);
        assertThat(result.getStatusCode().value()).isEqualTo(201);
        assertThat(result.getBody()).isEqualTo(response);
    }

    @Test
    void update_controller_should_call_service() {
        ClientSubscription response = ClientSubscription.builder().clientSubscriptionId(subscriptionId).build();
        when(subscriptionService.updateSubscription(subscriptionId, request)).thenReturn(response);
        var result = subscriptionController.updateClientSubscription(subscriptionId, request);
        verify(subscriptionService).updateSubscription(subscriptionId, request);
        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isEqualTo(response);
    }

    @Test
    void get_controller_should_call_service() {
        ClientSubscription response = ClientSubscription.builder().clientSubscriptionId(subscriptionId).build();
        when(subscriptionService.getSubscription(subscriptionId)).thenReturn(response);
        var result = subscriptionController.getClientSubscription(subscriptionId);
        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isEqualTo(response);
    }

    @Test
    void delete_controller_should_call_service() {
        var result = subscriptionController.deleteClientSubscription(subscriptionId);
        verify(subscriptionService).deleteSubscription(subscriptionId);
        assertThat(result.getStatusCode().value()).isEqualTo(204);
    }
}