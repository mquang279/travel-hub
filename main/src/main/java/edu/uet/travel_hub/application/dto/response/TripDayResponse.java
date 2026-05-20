package edu.uet.travel_hub.application.dto.response;

import java.time.LocalDate;
import java.util.List;

public record TripDayResponse(
        Long id,
        Long tripId,
        LocalDate date,
        int dayNumber,
        List<TripActivityResponse> activities) {
}
