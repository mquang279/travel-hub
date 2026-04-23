package edu.uet.travel_hub.domain.model;

import lombok.Builder;


@Builder
public record TravelPlaceRecommendationModel(
        Long travelPlaceId,
        double score,
        double preferenceScore,
        double historyScore,
        double popularityScore) {
}
