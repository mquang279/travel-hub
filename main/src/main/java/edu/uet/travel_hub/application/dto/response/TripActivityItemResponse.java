package edu.uet.travel_hub.application.dto.response;

import java.time.Instant;

public record TripActivityItemResponse(
        String type,
        String description,
        String actorName,
        Instant createdAt) {
}
