package edu.uet.travel_hub.infrastructure.persistence.repository.jpa;

public interface TravelPlaceReviewStatsProjection {
    Double getAverageRating();

    Long getReviewCount();
}
