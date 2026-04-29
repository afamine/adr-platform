package com.adrplatform.auth.exception;

public class TokenExpiredException extends InvalidTokenException {
    public TokenExpiredException(String message) {
        super(message, "EXPIRED");
    }
}
