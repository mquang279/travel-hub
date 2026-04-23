package edu.uet.travel_hub.domain.dto.response;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PreferenceResponse(
        @JsonProperty("user_id")
        Long userId,
        @JsonProperty("trip_type")
        String tripType,
        List<String> interests,
        String destination,
        @JsonProperty("updated_at")
        Instant updatedAt,
        Boolean isOnboarded) {
}
