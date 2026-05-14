package edu.uet.travel_hub.infrastructure.persistence.repository.jpa;

import java.time.Instant;

public interface ItinerarySummaryProjection {
    Long getId();

    String getGroupName();

    int getVersion();

    long getTotalDays();

    long getTotalStops();

    Instant getUpdatedAt();
}
