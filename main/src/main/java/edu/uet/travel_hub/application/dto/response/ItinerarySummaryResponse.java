package edu.uet.travel_hub.application.dto.response;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ItinerarySummaryResponse(
        Long id,
        @JsonProperty("group_name")
        String groupName,
        int version,
        @JsonProperty("total_days")
        int totalDays,
        @JsonProperty("total_stops")
        int totalStops,
        @JsonProperty("updated_at")
        Instant updatedAt) {
}
