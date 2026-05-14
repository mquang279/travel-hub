package edu.uet.travel_hub.application.dto.response;

import java.time.Instant;

public record TripActivityLogResponse(
        Long id,
        String actionType,
        String targetType,
        Long targetId,
        String description,
        Instant createdAt) {
}