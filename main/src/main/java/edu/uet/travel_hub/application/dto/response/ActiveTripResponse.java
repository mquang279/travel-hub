package edu.uet.travel_hub.application.dto.response;

import java.time.LocalDate;

public record ActiveTripResponse(
        Long tripId,
        String name,
        String location,
        String coverImageUrl,
        LocalDate startDate,
        LocalDate endDate) {
}