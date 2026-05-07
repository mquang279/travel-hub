package edu.uet.travel_hub.application.dto.request;

import java.time.LocalDate;

public record CreateTripRequest(
        String name,
        String destination,
        LocalDate startDate,
        LocalDate endDate,
        Double budgetMin,
        Double budgetMax
) {
}