package com.adrplatform.adr.exception;

/** Raised when a user exceeds the AI draft-generation request limit. */
public class AiDraftRateLimitException extends RuntimeException {
    public AiDraftRateLimitException() {
        super("Too many AI draft requests. Please wait a minute and try again.");
    }
}
