package com.adrplatform.vote.exception;

public class InvalidVoteException extends RuntimeException {

    private static final String ERROR_TYPE = "INVALID_VOTE";

    public InvalidVoteException(String message) {
        super(message);
    }

    public String getErrorType() {
        return ERROR_TYPE;
    }
}
