package edu.uet.travel_hub.application.dto.response;

import java.util.List;

public record TripExpenseResponse(
        TripExpenseSummaryResponse summary,
        List<TripExpenseContributionResponse> contributions,
        List<TripExpenseTransactionResponse> transactions) {
}