package com.eia.party.domain;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(PartyApiException.class)
    public ResponseEntity<ApiError> partyApiException(PartyApiException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(new ApiError(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiError> missingHeader(MissingRequestHeaderException ex) {
        if ("X-EIA-Tenant".equalsIgnoreCase(ex.getHeaderName())) {
            return badRequest("TENANT_REQUIRED", "X-EIA-Tenant header is required");
        }
        return badRequest("MISSING_HEADER", "Missing required header: " + ex.getHeaderName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> typeMismatch(MethodArgumentTypeMismatchException ex) {
        if ("tenantId".equals(ex.getName()) || "X-EIA-Tenant".equalsIgnoreCase(ex.getName())) {
            return badRequest("TENANT_INVALID", "X-EIA-Tenant header must be a UUID");
        }
        return badRequest("INVALID_ARGUMENT", "Invalid value for " + ex.getName());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
        Map<String, String> details = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            details.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.badRequest()
                .body(new ApiError("VALIDATION", "Request validation failed", details));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException ex) {
        return badRequest("VALIDATION", "Malformed or unreadable request body");
    }

    private static ResponseEntity<ApiError> badRequest(String code, String message) {
        return ResponseEntity.badRequest().body(new ApiError(code, message));
    }
}
