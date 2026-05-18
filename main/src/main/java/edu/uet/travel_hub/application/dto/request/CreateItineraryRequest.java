package edu.uet.travel_hub.application.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;

public record CreateItineraryRequest(
        @NotBlank(message = "groupName is required")
        String groupName,
        @JsonProperty("trip_id")
        Long tripId) {
}
