package com.adrplatform.vote.exception;

public class AlreadyVotedException extends RuntimeException {

    private static final String ERROR_TYPE = "ALREADY_VOTED";

    public AlreadyVotedException(String message) {
        super(message);
    }

    public String getErrorType() {
        return ERROR_TYPE;
    }
}
