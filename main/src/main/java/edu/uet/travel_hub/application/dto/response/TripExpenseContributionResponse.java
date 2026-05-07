package edu.uet.travel_hub.application.dto.response;

import java.math.BigDecimal;

public record TripExpenseContributionResponse(
        Long userId,
        String userName,
        String avatarUrl,
        BigDecimal amountPaid) {
}