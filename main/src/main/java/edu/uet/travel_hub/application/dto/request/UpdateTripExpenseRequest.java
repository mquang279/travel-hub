package edu.uet.travel_hub.application.dto.request;

import java.math.BigDecimal;
import java.util.List;

import edu.uet.travel_hub.domain.enums.ExpenseSource;
import edu.uet.travel_hub.domain.enums.TripExpenseCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateTripExpenseRequest(
        @NotBlank String title,
        @Positive BigDecimal amount,
        @Positive BigDecimal totalAmount,
        @NotNull TripExpenseCategory category,
        @NotNull Long paidByUserId,
        List<Long> splitUserIds,
        String note,
        String expenseDate,
        ExpenseSource source,
        String rawOcrText,
        String splitType) {
}
