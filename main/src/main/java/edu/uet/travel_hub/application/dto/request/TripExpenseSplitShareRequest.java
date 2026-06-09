package edu.uet.travel_hub.application.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TripExpenseSplitShareRequest(
        @NotNull Long userId,
        @Positive BigDecimal amount) {
}
