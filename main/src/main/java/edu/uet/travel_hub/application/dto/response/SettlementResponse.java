package edu.uet.travel_hub.application.dto.response;

import java.math.BigDecimal;

import edu.uet.travel_hub.domain.enums.SettlementStatus;

public record SettlementResponse(
        Long id,
        Long tripId,
        Long fromUserId,
        Long toUserId,
        BigDecimal amount,
        SettlementStatus status,
        String transferContent,
        SettlementReceiverResponse receiver) {
}
