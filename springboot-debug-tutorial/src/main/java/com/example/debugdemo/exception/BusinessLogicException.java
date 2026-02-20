package com.example.debugdemo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception for Business Logic Violations
 * 
 * Examples:
 * - Trying to order more items than available in stock
 * - Setting negative prices
 * - Invalid state transitions
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BusinessLogicException extends RuntimeException {

    private final String errorCode;
    private final Object[] args;

    public BusinessLogicException(String message) {
        super(message);
        this.errorCode = "BUSINESS_ERROR";
        this.args = new Object[]{};
    }

    public BusinessLogicException(String errorCode, String message, Object... args) {
        super(message);
        this.errorCode = errorCode;
        this.args = args;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Object[] getArgs() {
        return args;
    }
}
