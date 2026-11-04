package com.traceroute.exception;

import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.util.TreeMap;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    ResponseEntity<ApiError> status(org.springframework.web.server.ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(new ApiError(exception.getStatusCode().value(), exception.getReason()));
    }
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> invalid(IllegalArgumentException exception) { return ResponseEntity.badRequest().body(new ApiError(400, exception.getMessage())); }
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    ResponseEntity<ApiError> uploadTooLarge() { return ResponseEntity.status(413).body(new ApiError(413,"Evidence must be at most 5 MB")); }
    @ExceptionHandler(DuplicateEmailException.class)
    ResponseEntity<ApiError> duplicate(DuplicateEmailException exception) {
        return ResponseEntity.status(409).body(new ApiError(409, exception.getMessage()));
    }
    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiError> credentials() {
        return ResponseEntity.status(401).header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"TraceRoute\"")
                .body(new ApiError(401, "Invalid credentials"));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception) {
        var errors = new TreeMap<String, String>();
        exception.getBindingResult().getFieldErrors().forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ApiError(400, "Validation failed", errors));
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> malformed() { return ResponseEntity.badRequest().body(new ApiError(400, "Malformed JSON request")); }
}
