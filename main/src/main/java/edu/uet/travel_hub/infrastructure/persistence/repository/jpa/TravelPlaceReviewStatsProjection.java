package edu.uet.travel_hub.infrastructure.persistence.repository.jpa;

public interface TravelPlaceReviewStatsProjection {
    Long getPlaceId();

    Double getAverageRating();

    Long getReviewCount();
}
