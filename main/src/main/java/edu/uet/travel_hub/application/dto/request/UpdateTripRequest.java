package edu.uet.travel_hub.application.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateTripRequest(
        @NotBlank String name,
        @NotBlank String destination,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        BigDecimal budgetMin,
        BigDecimal budgetMax) {
}
