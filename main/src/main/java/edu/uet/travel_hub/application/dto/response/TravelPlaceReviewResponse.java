package edu.uet.travel_hub.application.dto.response;

import java.time.Instant;

public record TravelPlaceReviewResponse(
        Long id,
        TravelPlaceReviewAuthorResponse user,
        int rating,
        String content,
        Instant createdAt,
        Instant updatedAt) {
}
