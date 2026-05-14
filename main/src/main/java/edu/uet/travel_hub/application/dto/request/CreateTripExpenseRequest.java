package edu.uet.travel_hub.application.dto.request;

import java.math.BigDecimal;

import edu.uet.travel_hub.domain.enums.TripExpenseCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateTripExpenseRequest(
        @NotBlank String title,
        @NotNull @Positive BigDecimal amount,
        @NotNull TripExpenseCategory category,
        @NotNull Long paidByUserId) {
}