package edu.uet.travel_hub.application.dto.response;

public record SettlementReceiverResponse(
        Long userId,
        String bankCode,
        String bankName,
        String accountNumber,
        String accountName) {
}
