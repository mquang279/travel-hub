package edu.uet.travel_hub.application.dto.response;

import java.time.Instant;

public record BankAccountResponse(
        Long id,
        String bankCode,
        String bankName,
        String accountNumber,
        String accountName,
        Boolean isDefault,
        Instant createdAt,
        Instant updatedAt) {
}
