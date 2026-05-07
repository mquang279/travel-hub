package edu.uet.travel_hub.application.exception;

public class ForbiddenTripActionException extends RuntimeException {
    public ForbiddenTripActionException(String message) {
        super(message);
    }
}