package edu.uet.travel_hub.application.exception;

public class InviteCodeGenerationException extends RuntimeException {
    public InviteCodeGenerationException(String message) {
        super(message);
    }

    public InviteCodeGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
