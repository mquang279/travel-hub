package edu.uet.travel_hub.domain.model;

import java.time.Instant;

import lombok.Builder;

@Builder
public record ItinerarySummaryModel(
        Long id,
        String groupName,
        int version,
        long totalDays,
        long totalStops,
        Instant updatedAt) {
}
