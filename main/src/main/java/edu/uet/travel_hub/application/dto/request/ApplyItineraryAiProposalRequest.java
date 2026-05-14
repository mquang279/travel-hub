package edu.uet.travel_hub.application.dto.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record ApplyItineraryAiProposalRequest(
        @NotEmpty(message = "selectedChangeIds is required")
        @JsonProperty("selected_change_ids")
        List<String> selectedChangeIds,
        @NotNull(message = "baseVersion is required")
        @JsonProperty("base_version")
        Integer baseVersion) {
}
