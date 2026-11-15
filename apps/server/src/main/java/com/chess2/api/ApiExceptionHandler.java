package com.chess2.api;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<Map<String, Object>> domain(ApiException error, HttpServletRequest request) { return response(error.status(), error.getMessage(), request); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException error, HttpServletRequest request) {
        String message = error.getBindingResult().getFieldErrors().stream().findFirst().map(e -> e.getField() + " " + e.getDefaultMessage()).orElse("Invalid request");
        return response(HttpStatus.BAD_REQUEST, message, request);
    }
    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<Map<String, Object>> conflict(IllegalStateException error, HttpServletRequest request) { return response(HttpStatus.CONFLICT, error.getMessage(), request); }
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Map<String, Object>> forbidden(AccessDeniedException error, HttpServletRequest request) { return response(HttpStatus.FORBIDDEN, "Access denied", request); }
    private ResponseEntity<Map<String, Object>> response(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(Map.of("timestamp", Instant.now().toString(), "status", status.value(), "error", message, "path", request.getRequestURI()));
    }
}
