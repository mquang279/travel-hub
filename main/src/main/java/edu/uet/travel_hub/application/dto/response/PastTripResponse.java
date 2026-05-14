package edu.uet.travel_hub.application.dto.response;

public record PastTripResponse(
        Long tripId,
        String locationName,
        String dateString,
        String imageUrl) {
}