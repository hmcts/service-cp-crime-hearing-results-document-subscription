package uk.gov.hmcts.cp.subscription.controllers;

import jakarta.persistence.EntityNotFoundException;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import uk.gov.hmcts.cp.openapi.model.ErrorResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    GlobalExceptionHandler globalExceptionHandler;


    @Test
    void not_found_exceptions_should_handle_ok() {
        NoHandlerFoundException e = new NoHandlerFoundException("GET", "Url", null);
        ResponseEntity<String> response = globalExceptionHandler.handleNotFoundException(e);
        assertErrorFields(response, NOT_FOUND, "No endpoint GET Url.");

        EntityNotFoundException e2 = new EntityNotFoundException("Not found message", null);
        ResponseEntity<String> response2 = globalExceptionHandler.handleNotFoundException(e2);
        assertErrorFields(response2, NOT_FOUND, "Not found message");
    }

    @Test
    void error_response_should_handle_ok() {
        ResponseStatusException e = new ResponseStatusException(INTERNAL_SERVER_ERROR, "Error");
        ResponseEntity<String> response = globalExceptionHandler.handleResponseStatusException(e);
        assertErrorFields(response, INTERNAL_SERVER_ERROR, "Error");
    }

    @Test
    void client_exception_should_handle_ok() {
        HttpClientErrorException e = new HttpClientErrorException(NOT_FOUND, "Error");
        ResponseEntity<String> response = globalExceptionHandler.handleClientException(e);
        assertErrorFields(response, NOT_FOUND, "404 Error");
    }

    @Test
    void generic_exception_should_handle_ok() {
        Exception e = new Exception("message");
        ResponseEntity<String> response = globalExceptionHandler.handleUnknownException(e);
        assertErrorFields(response, INTERNAL_SERVER_ERROR, "message");
    }

    @Test
    void unsupported_media_type_should_handle_ok() {
        HttpMediaTypeNotSupportedException e =
                new HttpMediaTypeNotSupportedException("application/xml");

        ResponseEntity<String> response =
                globalExceptionHandler.handleHttpMediaTypeNotSupportedException(e);

        assertErrorFields(response, HttpStatus.UNSUPPORTED_MEDIA_TYPE, e.getMessage());
    }

    @Test
    void unsupported_operation_should_handle_ok() {
        UnsupportedOperationException e = new UnsupportedOperationException("not implemented");

        ResponseEntity<String> response =
                globalExceptionHandler.handleUnsupportedOperation(e);

        assertErrorFields(response, HttpStatus.NOT_IMPLEMENTED, "Unsupported");
    }

    @Test
    void condition_timeout_should_handle_ok() {
        ConditionTimeoutException e = new ConditionTimeoutException("timed out");

        ResponseEntity<String> response =
                globalExceptionHandler.handleConditionTimeout(e);

        assertErrorFields(response, HttpStatus.GATEWAY_TIMEOUT, "Material metadata not ready");
    }

    private void assertErrorFields(ResponseEntity<String> errorResponse, HttpStatusCode httpStatusCode, String message) {
        assertThat(errorResponse.getStatusCode()).isEqualTo(httpStatusCode);
        assertThat(errorResponse.getBody()).isEqualTo(message);
    }
}
