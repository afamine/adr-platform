package com.adrplatform.adr.exception;

/** Raised when the external AI provider cannot return a safe, usable ADR draft. */
public class AiDraftGenerationException extends RuntimeException {
    public AiDraftGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
