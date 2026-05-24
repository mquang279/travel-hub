package edu.uet.travel_hub.infrastructure.persistence.repository.jpa;

public interface TopTravelerStatsProjection {
    Long getId();

    String getUsername();

    String getName();

    String getAvatarUrl();

    Long getScore();

    Boolean getFollowing();

    Boolean getCurrentUser();
}
