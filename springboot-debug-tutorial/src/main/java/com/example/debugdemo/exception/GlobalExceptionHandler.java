package com.example.debugdemo.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Global Exception Handler
 * 
 * DEBUGGING TIPS:
 * 
 * 1. CENTRALIZED EXCEPTION HANDLING:
 *    - All exceptions pass through here
 *    - Great place to add logging and debugging info
 *    - Set breakpoints here to catch any unhandled exception
 * 
 * 2. ERROR RESPONSE STRUCTURE:
 *    - Include timestamp, error code, message
 *    - Add a unique error ID for log correlation
 *    - In dev: include stack trace; in prod: hide it
 * 
 * 3. LOGGING STRATEGY:
 *    - Log ERROR for unexpected exceptions
 *    - Log WARN for business exceptions
 *    - Log DEBUG for validation errors
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle Product Not Found
     */
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProductNotFound(
            ProductNotFoundException ex, WebRequest request) {
        
        String errorId = generateErrorId();
        
        // DEBUGGING: Log with error ID for correlation
        logger.warn("Error ID: {} - Product not found: {}", errorId, ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
                errorId,
                HttpStatus.NOT_FOUND.value(),
                "NOT_FOUND",
                ex.getMessage(),
                request.getDescription(false)
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handle Business Logic Exceptions
     */
    @ExceptionHandler(BusinessLogicException.class)
    public ResponseEntity<ErrorResponse> handleBusinessLogic(
            BusinessLogicException ex, WebRequest request) {
        
        String errorId = generateErrorId();
        
        logger.warn("Error ID: {} - Business logic error: {} - Code: {}", 
                errorId, ex.getMessage(), ex.getErrorCode());
        
        ErrorResponse error = new ErrorResponse(
                errorId,
                HttpStatus.BAD_REQUEST.value(),
                ex.getErrorCode(),
                ex.getMessage(),
                request.getDescription(false)
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle Validation Errors
     * 
     * DEBUGGING TIP: This catches @Valid annotation failures.
     * Check which fields failed and why.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex) {
        
        String errorId = generateErrorId();
        
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
            
            // DEBUGGING: Log each validation failure
            logger.debug("Error ID: {} - Validation failed for field '{}': {}", 
                    errorId, fieldName, errorMessage);
        });
        
        ValidationErrorResponse error = new ValidationErrorResponse(
                errorId,
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_FAILED",
                "Request validation failed",
                fieldErrors
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle All Other Exceptions
     * 
     * DEBUGGING TIP: This is your safety net.
     * Set a breakpoint here to catch any unexpected exception.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllExceptions(
            Exception ex, WebRequest request) {
        
        String errorId = generateErrorId();
        
        // DEBUGGING: Log full stack trace for unexpected errors
        logger.error("Error ID: {} - Unexpected error occurred", errorId, ex);
        
        ErrorResponse error = new ErrorResponse(
                errorId,
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_ERROR",
                "An unexpected error occurred. Error ID: " + errorId,
                request.getDescription(false)
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Generate unique error ID for log correlation
     * DEBUGGING TIP: Use this ID to find logs related to a specific error
     */
    private String generateErrorId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // =========================================================
    // ERROR RESPONSE DTOs
    // =========================================================

    public static class ErrorResponse {
        private final String errorId;
        private final LocalDateTime timestamp;
        private final int status;
        private final String code;
        private final String message;
        private final String path;

        public ErrorResponse(String errorId, int status, String code, 
                           String message, String path) {
            this.errorId = errorId;
            this.timestamp = LocalDateTime.now();
            this.status = status;
            this.code = code;
            this.message = message;
            this.path = path;
        }

        // Getters
        public String getErrorId() { return errorId; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public int getStatus() { return status; }
        public String getCode() { return code; }
        public String getMessage() { return message; }
        public String getPath() { return path; }
    }

    public static class ValidationErrorResponse extends ErrorResponse {
        private final Map<String, String> fieldErrors;

        public ValidationErrorResponse(String errorId, int status, String code,
                                       String message, Map<String, String> fieldErrors) {
            super(errorId, status, code, message, "");
            this.fieldErrors = fieldErrors;
        }

        public Map<String, String> getFieldErrors() { return fieldErrors; }
    }
}
