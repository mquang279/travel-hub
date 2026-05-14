package edu.uet.travel_hub.application.dto.response;

import java.math.BigDecimal;

public record TripExpenseSummaryResponse(
        BigDecimal totalAmount,
        BigDecimal perPersonAmount,
        BigDecimal myBalance) {
}