package uk.gov.hmcts.cp.subscription.controllers;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String NOT_FOUND_MESSAGE = "No row with the given identifier exists";

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<String> handleNotFoundException(final EntityNotFoundException exception) {
        log.error("NotFoundException {}", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(NOT_FOUND_MESSAGE);
    }

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<String> handleClientException(final HttpClientErrorException exception) {
        log.error("HttpClientErrorException {}", exception.getMessage());
        return ResponseEntity
                .status(exception.getStatusCode())
                .body(exception.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> handleHttpMessageNotReadableException(final HttpMessageNotReadableException exception) {
        log.error("Invalid request body - HttpMessageNotReadableException: {}", exception.getMessage(), exception);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body("");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleUnknownException(final Exception exception) {
        log.error("Exception {}", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(exception.getMessage());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<String> handleHttpMediaTypeNotSupportedException(
            final HttpMediaTypeNotSupportedException exception) {
        log.error("Unsupported media type - HttpMediaTypeNotSupportedException: {}", exception.getMessage(), exception);
        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(exception.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Void> handleConstraintViolation(final ConstraintViolationException ex) {
        log.error("Exception {}", ex.getMessage());
        return ResponseEntity.badRequest().build();
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<String> handleNoHandlerFoundException(final NoHandlerFoundException exception) {
        log.error("No handler found for request: {} {}", exception.getHttpMethod(), exception.getRequestURL(), exception);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not Found");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> handleResponseStatusException(final ResponseStatusException exception) {
        log.error("ResponseStatusException: {}", exception.getMessage());
        return ResponseEntity
                .status(exception.getStatusCode())
                .body(exception.getReason() != null ? exception.getReason() : exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidationException(final MethodArgumentNotValidException exception) {
        log.error("Validation failed: {}", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(exception.getMessage());
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<String> handleUnsupportedOperation(final UnsupportedOperationException exception) {
        log.error("Unsupported operation: {}", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_IMPLEMENTED)
                .body("Unsupported");
    }

}