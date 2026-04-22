package edu.uet.travel_hub.application.dto.response;

public record TravelPlaceListItemResponse(
        Long id,
        String name,
        String description,
        ProvinceResponse province,
        String mainImage,
        Integer views,
        String openingTime,
        double averageRating,
        long reviewCount) {
}
