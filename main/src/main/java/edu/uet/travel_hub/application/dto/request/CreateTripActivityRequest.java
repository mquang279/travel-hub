package edu.uet.travel_hub.application.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTripActivityRequest(
        @NotNull LocalDate date,
        @NotBlank String title,
        String description,
        LocalTime startTime,
        LocalTime endTime,
        String locationName,
        String address,
        String type,
        Integer orderIndex,
        BigDecimal estimatedCost) {
}
