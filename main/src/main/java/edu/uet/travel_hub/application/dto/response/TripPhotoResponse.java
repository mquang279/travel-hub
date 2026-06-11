package edu.uet.travel_hub.application.dto.response;

import java.time.Instant;

public record TripPhotoResponse(
        Long id,
        String imageUrl,
        Long uploadedByUserId,
        String uploadedByName,
        Instant uploadedAt) {
}
