package edu.uet.travel_hub.application.dto.response;

import java.util.List;

public record TripDashboardResponse(
        ActiveTripResponse activeTrip,
        List<UpcomingTripResponse> upcomingTrips,
        List<PastTripResponse> pastTrips
) {
    public record ActiveTripResponse(
            Long tripId,
            String name,
            String location,
            String coverImageUrl,
            String startDate,
            String endDate) {
    }

    public record UpcomingTripResponse(
            Long tripId,
            String name,
            String location,
            String coverImageUrl,
            int daysLeft,
            int memberCount) {
    }

    public record PastTripResponse(
            Long tripId,
            String locationName,
            String dateString,
            String imageUrl) {
    }
}