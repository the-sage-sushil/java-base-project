package com.sushil.base_project.core.common;

import java.util.Map;
import java.util.LinkedHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.sushil.base_project.core.common.exceptions.DuplicateResourceException;

@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<Map<String, String>> handleBadCredentials(BadCredentialsException ex) {

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(Map.of(
                                                "message",
                                                "Invalid email or password"));
        }

        @ExceptionHandler(UsernameNotFoundException.class)
        public ResponseEntity<Map<String, String>> handleUsernameNotFound(UsernameNotFoundException ex) {

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(Map.of(
                                                "message",
                                                "Invalid email or password"));
        }

        @ExceptionHandler(AuthenticationException.class)
        public ResponseEntity<Map<String, String>> handleAuthenticationException(
                        AuthenticationException ex) {

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(Map.of(
                                                "message",
                                                "Invalid email or password"));
        }

        @ExceptionHandler(DuplicateResourceException.class)
        public ResponseEntity<Map<String, String>> handleDuplicateResourceException(
                        DuplicateResourceException ex) {

                return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(Map.of(
                                                "message",
                                                ex.getMessage()));

        }

        @ExceptionHandler(DataIntegrityViolationException.class)
        public ResponseEntity<Map<String, String>> handleDataIntegrityViolation(
                        DataIntegrityViolationException ex) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(Map.of("message", "The requested resource conflicts with existing data"));
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<Map<String, Object>> handleValidation(
                        MethodArgumentNotValidException ex) {
                Map<String, String> errors = new LinkedHashMap<>();
                ex.getBindingResult().getFieldErrors().forEach(error ->
                                errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
                return ResponseEntity.badRequest().body(Map.of(
                                "message", "Validation failed",
                                "errors", errors));

        }

}
