package edu.uet.travel_hub.application.dto.response;

import java.time.Instant;

public record TravelPlaceViewHistoryResponse(
        Long placeId,
        String placeName,
        String mainImage,
        String provinceName,
        Instant viewedAt) {
}
