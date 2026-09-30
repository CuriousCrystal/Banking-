package com.amc.bfsi.exception;

import jakarta.servlet.ServletException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Every failure in the application leaves through here, so the client
 * only ever has to understand one error shape.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(ResourceNotFoundException ex) {
        return body(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<Map<String, Object>> duplicate(DuplicateResourceException ex) {
        return body(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<Map<String, Object>> businessRule(BusinessRuleException ex) {
        return body(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), null);
    }

    // the bank's subscription plan does not allow this
    @ExceptionHandler(PlanLimitException.class)
    public ResponseEntity<Map<String, Object>> planLimit(PlanLimitException ex) {
        return body(HttpStatus.PAYMENT_REQUIRED, ex.getMessage(), null);
    }

    // a unique constraint lost a race (e.g. two signups for the same bank code at once) - never leak the SQL
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> integrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation", ex);
        return body(HttpStatus.CONFLICT, "The change conflicts with existing data", null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fields.put(error.getField(), error.getDefaultMessage());
        }
        return body(HttpStatus.BAD_REQUEST, "Validation failed", fields);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> unreadable(HttpMessageNotReadableException ex) {
        return body(HttpStatus.BAD_REQUEST, "Request body is missing or malformed", null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> denied(AccessDeniedException ex) {
        return body(HttpStatus.FORBIDDEN, "Your role is not allowed to perform this action", null);
    }

    // two requests updated the same row concurrently - the loser gets 409 and can retry
    @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> optimisticLock(
            org.springframework.orm.ObjectOptimisticLockingFailureException ex) {
        return body(HttpStatus.CONFLICT,
                "This record was just modified by another transaction - reload and retry", null);
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> methodNotAllowed(
            org.springframework.web.HttpRequestMethodNotSupportedException ex) {
        return body(HttpStatus.METHOD_NOT_ALLOWED,
                ex.getMethod() + " is not supported on this URL - is the backend rebuilt?", null);
    }

    // Spring MVC's own "the client got it wrong" errors (unknown URL, bad parameter type, unsupported
    // media type ...) carry their own status - without this they would all fall into the 500 below
    @ExceptionHandler({ServletException.class, TypeMismatchException.class})
    public ResponseEntity<Map<String, Object>> mvcClientError(Exception ex) {
        HttpStatus status = ex instanceof ErrorResponse er
                ? HttpStatus.resolve(er.getStatusCode().value()) : HttpStatus.BAD_REQUEST;
        if (status == null || status.is5xxServerError()) {
            return everythingElse(ex);
        }
        return body(status, ex.getMessage(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> everythingElse(Exception ex) {
        // the details go to the log, the client only gets a reference to quote to support
        String reference = UUID.randomUUID().toString().substring(0, 8);
        log.error("Unhandled error, reference {}", reference, ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR,
                "Something went wrong on our side (reference " + reference + ")", null);
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String message,
                                                     Map<String, String> fields) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("timestamp", LocalDateTime.now().toString());
        out.put("status", status.value());
        out.put("error", status.getReasonPhrase());
        out.put("message", message);
        out.put("errors", fields == null ? Map.of() : fields);
        return ResponseEntity.status(status).body(out);
    }
}
