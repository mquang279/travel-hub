package edu.uet.travel_hub.application.dto.response;

public record TripDetailHighlightsResponse(
        TripDetailTopExpenseResponse topExpense,
        TripDetailWinningPollResponse winningPoll) {
}
