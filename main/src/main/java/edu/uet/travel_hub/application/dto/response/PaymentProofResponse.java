package edu.uet.travel_hub.application.dto.response;

import java.time.Instant;

public record PaymentProofResponse(
        Long id,
        Long settlementId,
        Long uploadedByUserId,
        String imageUrl,
        String note,
        Instant uploadedAt) {
}
