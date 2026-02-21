package com.blackrock.challenge.exception;

import com.blackrock.challenge.dto.ApiErrorResponse;
import com.blackrock.challenge.util.DateTimeUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Global exception handler for the BlackRock Challenge API.
 * Converts all exceptions to a consistent ApiErrorResponse format.
 *
 * <p>Handles:
 * - Validation errors (@Valid failures)
 * - Illegal argument exceptions (bad date formats, invalid input)
 * - General unhandled exceptions (500)
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles @Valid / @Validated constraint violations.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        log.warn("Validation failed for {}: {}", request.getRequestURI(), message);

        return ResponseEntity.badRequest().body(ApiErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("VALIDATION_ERROR")
                .message(message)
                .path(request.getRequestURI())
                .timestamp(DateTimeUtil.format(LocalDateTime.now()))
                .build());
    }

    /**
     * Handles malformed JSON request bodies.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMalformedJson(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        log.warn("Malformed JSON for {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.badRequest().body(ApiErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("MALFORMED_REQUEST")
                .message("Invalid JSON: " + ex.getMostSpecificCause().getMessage())
                .path(request.getRequestURI())
                .timestamp(DateTimeUtil.format(LocalDateTime.now()))
                .build());
    }

    /**
     * Handles invalid date formats and bad business logic inputs.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        log.warn("Bad request for {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.badRequest().body(ApiErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("BAD_REQUEST")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(DateTimeUtil.format(LocalDateTime.now()))
                .build());
    }

    /**
     * Catch-all for unexpected server errors.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericError(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unhandled exception for {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        return ResponseEntity.internalServerError().body(ApiErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("INTERNAL_SERVER_ERROR")
                .message("An unexpected error occurred. Please contact support.")
                .path(request.getRequestURI())
                .timestamp(DateTimeUtil.format(LocalDateTime.now()))
                .build());
    }
}
