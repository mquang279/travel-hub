package edu.uet.travel_hub.domain.model;

import java.util.List;

import lombok.Builder;


@Builder
public record TravelPlaceRecommendationQuery(
        Long userId,
        List<Long> viewedPlaceIds,
        Long provinceId,
        int limit,
        int offset) {
}
