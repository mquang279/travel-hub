package edu.uet.travel_hub.application.dto.response;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ItineraryResponse(
        Long id,
        @JsonProperty("group_name")
        String groupName,
        int version,
        @JsonProperty("owner_id")
        Long ownerId,
        List<ItineraryDayResponse> days,
        @JsonProperty("created_at")
        Instant createdAt,
        @JsonProperty("updated_at")
        Instant updatedAt) {
}
