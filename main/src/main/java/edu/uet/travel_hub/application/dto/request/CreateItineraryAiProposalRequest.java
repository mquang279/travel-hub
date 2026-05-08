package edu.uet.travel_hub.application.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;

public record CreateItineraryAiProposalRequest(
        @NotBlank(message = "prompt is required")
        String prompt,
        @JsonProperty("input_type")
        String inputType,
        @JsonProperty("selected_day_id")
        Long selectedDayId,
        @JsonProperty("selected_day_index")
        Integer selectedDayIndex,
        @JsonProperty("desired_days")
        Integer desiredDays,
        String destination,
        String task,
        @JsonProperty("base_version")
        Integer baseVersion) {
}
