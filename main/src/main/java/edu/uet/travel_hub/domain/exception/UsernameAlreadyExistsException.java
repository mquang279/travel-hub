package edu.uet.travel_hub.domain.exception;

public class UsernameAlreadyExistsException extends RuntimeException {
    public UsernameAlreadyExistsException() {
        super("Username already exists.");
    }

    public UsernameAlreadyExistsException(String username) {
        super("Username already exists: " + username);
    }

    public UsernameAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
