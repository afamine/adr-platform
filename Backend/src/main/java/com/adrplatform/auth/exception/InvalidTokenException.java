package com.adrplatform.auth.exception;

public class InvalidTokenException extends RuntimeException {

    private final String errorType;

    public InvalidTokenException(String message) {
        this(message, "INVALID");
    }

    public InvalidTokenException(String message, String errorType) {
        super(message);
        this.errorType = errorType;
    }

    public String getErrorType() {
        return errorType;
    }
}
