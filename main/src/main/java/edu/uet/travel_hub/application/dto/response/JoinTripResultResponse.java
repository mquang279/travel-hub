package edu.uet.travel_hub.application.dto.response;

public record JoinTripResultResponse(
        Long tripId,
        String status,
        String message) {
}
