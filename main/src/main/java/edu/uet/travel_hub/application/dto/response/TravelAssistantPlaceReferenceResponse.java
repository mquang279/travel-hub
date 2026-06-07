package edu.uet.travel_hub.application.dto.response;

public record TravelAssistantPlaceReferenceResponse(
        Long id,
        String name,
        String province,
        Double averageRating,
        long reviewCount) {
}
