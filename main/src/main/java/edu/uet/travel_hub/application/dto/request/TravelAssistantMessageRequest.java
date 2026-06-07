package edu.uet.travel_hub.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TravelAssistantMessageRequest(
        @NotBlank String role,
        @NotBlank String content) {
}
