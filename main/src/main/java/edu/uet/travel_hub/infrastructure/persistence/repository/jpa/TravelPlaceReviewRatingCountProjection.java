package edu.uet.travel_hub.infrastructure.persistence.repository.jpa;

public interface TravelPlaceReviewRatingCountProjection {
    Integer getRating();

    Long getReviewCount();
}
