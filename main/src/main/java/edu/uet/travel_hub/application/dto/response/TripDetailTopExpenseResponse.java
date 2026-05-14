package edu.uet.travel_hub.application.dto.response;

import java.math.BigDecimal;

public record TripDetailTopExpenseResponse(
        String title,
        BigDecimal amount) {
}
