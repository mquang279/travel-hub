package edu.uet.travel_hub.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateItineraryRequest(
        @NotBlank(message = "groupName is required")
        String groupName) {
}
