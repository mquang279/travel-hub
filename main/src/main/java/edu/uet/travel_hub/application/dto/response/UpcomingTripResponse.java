package edu.uet.travel_hub.application.dto.response;

public record UpcomingTripResponse(
        Long tripId,
        String name,
        String location,
        String coverImageUrl,
        int daysLeft,
        int memberCount) {
}