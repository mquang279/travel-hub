package edu.uet.travel_hub.domain.model;

import java.util.List;

import lombok.Builder;


@Builder
public record PostEmbeddingSyncModel(
        Long postId,
        String description,
        List<String> imageUrls,
        Long travelPlaceId,
        String travelPlaceName,
        String travelPlaceDescription,
        String provinceName,
        String openingTime,
        Double lat,
        Double lon) {
}
