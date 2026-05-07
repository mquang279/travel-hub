package edu.uet.travel_hub.application.dto.response;

import java.time.Instant;

public record TripJoinRequestResponse(
        Long userId,
        String name,
        String avatarUrl,
        Instant requestedAt) {
}