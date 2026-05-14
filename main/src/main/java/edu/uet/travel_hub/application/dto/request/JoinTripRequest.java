package edu.uet.travel_hub.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinTripRequest(
        @NotBlank
        @Size(min = 8, max = 8)
        String inviteCode) {
}