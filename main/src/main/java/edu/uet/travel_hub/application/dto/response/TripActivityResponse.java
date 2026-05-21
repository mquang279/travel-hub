package edu.uet.travel_hub.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalTime;

public record TripActivityResponse(
        Long id,
        Long tripDayId,
        String title,
        String description,
        LocalTime startTime,
        LocalTime endTime,
        String locationName,
        String address,
        String type,
        int orderIndex,
        BigDecimal estimatedCost) {
}
