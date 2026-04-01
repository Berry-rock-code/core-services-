package com.berryrock.integrationhub.controller;
// LAYER: PLATFORM -- stays in integration-hub

import com.berryrock.integrationhub.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Centralized exception handler for all REST controllers in this service.
 *
 * Part of the controller layer — intercepts exceptions thrown by any
 * {@code @RestController} and translates them into structured {@link ErrorResponse}
 * objects with appropriate HTTP status codes. This prevents raw stack traces from
 * reaching API callers and ensures a consistent error envelope across all endpoints.
 *
 * Handled exception types:
 * <ul>
 *   <li>{@link MethodArgumentNotValidException} — Spring Bean Validation failures on
 *       request bodies; returns HTTP 400 with field-level error details</li>
 *   <li>{@link IllegalArgumentException} — explicit bad-input rejections from service
 *       code; returns HTTP 400 with the exception message</li>
 *   <li>{@link Exception} — catch-all for any other unhandled exception;
 *       returns HTTP 500 with a generic message</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler
{
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles Spring Bean Validation failures ({@code @Valid} on request bodies).
     *
     * Extracts field-level constraint violation messages from the binding result and
     * includes them in the {@link ErrorResponse#getDetails()} list so callers can
     * identify exactly which fields failed validation.
     *
     * @param ex the validation exception thrown by the framework
     * @return {@code 400 Bad Request} with error code {@code ERR_VALIDATION}
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex)
    {
        log.warn("Validation error: {}", ex.getMessage());

        // Extract field-name + message pairs from the binding result
        List<String> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        ErrorResponse errorResponse = new ErrorResponse("Validation Failed", "ERR_VALIDATION", details);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles explicit bad-input rejections thrown by service code.
     *
     * Service methods throw {@link IllegalArgumentException} when the caller provides
     * logically invalid input that Bean Validation cannot catch (e.g., a blank sheet name).
     *
     * @param ex the exception with a human-readable message describing the problem
     * @return {@code 400 Bad Request} with error code {@code ERR_BAD_REQUEST}
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex)
    {
        log.warn("Illegal argument: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(ex.getMessage(), "ERR_BAD_REQUEST", Collections.emptyList());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Catch-all handler for any exception not handled by a more specific method.
     *
     * Logs the full exception at ERROR level for diagnosis but does not expose internal
     * details to the caller.
     *
     * @param ex the unhandled exception
     * @return {@code 500 Internal Server Error} with error code {@code ERR_INTERNAL_SERVER_ERROR}
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllUncaughtException(Exception ex)
    {
        log.error("Unknown error occurred", ex);
        ErrorResponse errorResponse = new ErrorResponse(
                "An unexpected error occurred",
                "ERR_INTERNAL_SERVER_ERROR",
                Collections.emptyList()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
