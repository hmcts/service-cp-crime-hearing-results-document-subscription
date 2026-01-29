package uk.gov.hmcts.cp.subscription.controllers;

import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;

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
    public ResponseEntity<String> handleHttpMessageNotReadable(final HttpMessageNotReadableException exception) {
        log.error("Invalid request body: {}", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body("");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleUnknownException(final Exception exception) {
        log.error("Exception {}", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(exception.getMessage());
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<String> handleFeignException(final FeignException ex) {
        log.error("FeignException from downstream service", ex);
        HttpStatus status = HttpStatus.resolve(ex.status());
        if (status == null) {
            status = HttpStatus.BAD_GATEWAY;
        }
        return ResponseEntity
                .status(status)
                .body(ex.contentUTF8());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Void> handleConstraintViolation(final ConstraintViolationException ex) {
        log.error("Exception {}", ex.getMessage());
        return ResponseEntity.badRequest().build();
    }
}