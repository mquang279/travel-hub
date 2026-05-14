package edu.uet.travel_hub.application.dto.request;

import edu.uet.travel_hub.domain.enums.TripPollCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateTripPollRequest(
        @NotBlank String title,
        @NotNull TripPollCategory category) {
}
