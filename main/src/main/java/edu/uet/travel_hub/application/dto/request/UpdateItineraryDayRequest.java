package edu.uet.travel_hub.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateItineraryDayRequest(
        @NotBlank(message = "label is required")
        String label,
        @NotBlank(message = "dateLabel is required")
        String dateLabel) {
}
