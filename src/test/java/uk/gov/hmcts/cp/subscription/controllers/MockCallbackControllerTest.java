package uk.gov.hmcts.cp.subscription.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.cp.hmac.services.EncodingService;
import uk.gov.hmcts.cp.hmac.services.HmacSigningService;
import uk.gov.hmcts.cp.subscription.config.AppProperties;
import uk.gov.hmcts.cp.subscription.config.EnvironmentName;
import uk.gov.hmcts.cp.vault.SecretStoreServiceInterface;

import java.security.InvalidKeyException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MockCallbackControllerTest {

    private static final String KEY_ID = "kid-v1-test-key";
    private static final String ENCODED_SECRET = "dGVzdC1zZWNyZXQtMzItYnl0ZXMtbG9uZw==";
    private static final byte[] DECODED_SECRET = "test-secret-32-bytes-long".getBytes();
    private static final String SIGNATURE = "validSignature==";
    private static final String BODY = "{\"eventId\":\"123\"}";

    @Mock
    private SecretStoreServiceInterface secretStoreService;

    @Mock
    private HmacSigningService hmacSigningService;

    @Mock
    private EncodingService encodingService;

    @Mock
    private AppProperties appProperties;

    @InjectMocks
    private MockCallbackController mockCallbackController;

    @Test
    void valid_signature_should_return_200() throws InvalidKeyException {
        when(appProperties.getEnvironmentName()).thenReturn(EnvironmentName.DEV);
        when(secretStoreService.getSecret(KEY_ID)).thenReturn(Optional.of(ENCODED_SECRET));
        when(encodingService.decodeFromBase64(ENCODED_SECRET)).thenReturn(DECODED_SECRET);
        doNothing().when(hmacSigningService).validateSignature(DECODED_SECRET, BODY, SIGNATURE);

        final ResponseEntity<Void> response = mockCallbackController.mockCallback(KEY_ID, SIGNATURE, BODY);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void sit_environment_should_return_200() throws InvalidKeyException {
        when(appProperties.getEnvironmentName()).thenReturn(EnvironmentName.SIT);
        when(secretStoreService.getSecret(KEY_ID)).thenReturn(Optional.of(ENCODED_SECRET));
        when(encodingService.decodeFromBase64(ENCODED_SECRET)).thenReturn(DECODED_SECRET);
        doNothing().when(hmacSigningService).validateSignature(DECODED_SECRET, BODY, SIGNATURE);

        final ResponseEntity<Void> response = mockCallbackController.mockCallback(KEY_ID, SIGNATURE, BODY);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void non_dev_or_sit_environment_should_return_404() {
        when(appProperties.getEnvironmentName()).thenReturn(EnvironmentName.PROD);

        final ResponseEntity<Void> response = mockCallbackController.mockCallback(KEY_ID, SIGNATURE, BODY);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void unknown_key_id_should_return_401() {
        when(appProperties.getEnvironmentName()).thenReturn(EnvironmentName.DEV);
        when(secretStoreService.getSecret(KEY_ID)).thenReturn(Optional.empty());

        final ResponseEntity<Void> response = mockCallbackController.mockCallback(KEY_ID, SIGNATURE, BODY);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void invalid_signature_should_return_401() throws InvalidKeyException {
        when(appProperties.getEnvironmentName()).thenReturn(EnvironmentName.DEV);
        when(secretStoreService.getSecret(KEY_ID)).thenReturn(Optional.of(ENCODED_SECRET));
        when(encodingService.decodeFromBase64(ENCODED_SECRET)).thenReturn(DECODED_SECRET);
        doThrow(new InvalidKeyException("Invalid signature")).when(hmacSigningService)
                .validateSignature(DECODED_SECRET, BODY, SIGNATURE);

        final ResponseEntity<Void> response = mockCallbackController.mockCallback(KEY_ID, SIGNATURE, BODY);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
