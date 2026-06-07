package edu.uet.travel_hub.application.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

public record TravelAssistantChatRequest(
        @NotBlank String message,
        List<TravelAssistantMessageRequest> history) {
}
