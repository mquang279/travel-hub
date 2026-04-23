package edu.uet.travel_hub.domain.dto.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PreferenceUpdateRequest(
        @JsonProperty("trip_type")
        String tripType,
        List<String> interests,
        String destination,
        Boolean isOnboarded) {
}
