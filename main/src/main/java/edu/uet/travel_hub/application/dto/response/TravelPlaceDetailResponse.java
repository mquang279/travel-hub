package edu.uet.travel_hub.application.dto.response;

import java.util.List;

public record TravelPlaceDetailResponse(
        Long id,
        String name,
        String description,
        Double lat,
        Double lon,
        Integer views,
        String openingTime,
        ProvinceResponse province,
        List<TravelPlaceImageResponse> images,
        TravelPlaceReviewSummaryResponse reviewSummary,
        TravelPlaceReviewResponse myReview) {
}
