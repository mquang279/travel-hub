package edu.uet.travel_hub.application.dto.response;

import java.util.Map;

public record TravelPlaceReviewListSummaryResponse(
        double averageRating,
        long reviewCount,
        Map<Integer, Long> ratingCounts) {
}
