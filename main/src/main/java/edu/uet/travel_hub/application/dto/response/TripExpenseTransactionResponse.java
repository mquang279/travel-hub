package edu.uet.travel_hub.application.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

import edu.uet.travel_hub.domain.enums.TripExpenseCategory;

public record TripExpenseTransactionResponse(
        Long id,
        String title,
        TripExpenseCategory category,
        Long paidByUserId,
        String paidByName,
        BigDecimal amount,
        Instant date,
        String proofImageUrl) {
}
