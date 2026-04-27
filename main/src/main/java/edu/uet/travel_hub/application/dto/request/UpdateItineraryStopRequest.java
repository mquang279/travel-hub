package edu.uet.travel_hub.application.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateItineraryStopRequest(
        @NotNull(message = "dayId is required")
        @JsonProperty("day_id")
        Long dayId,
        @JsonProperty("sort_order")
        Integer sortOrder,
        @JsonProperty("start_time")
        String startTime,
        @JsonProperty("end_time")
        String endTime,
        @NotBlank(message = "title is required")
        String title,
        @NotBlank(message = "placeName is required")
        @JsonProperty("place_name")
        String placeName,
        String note,
        @JsonProperty("transport_to_next")
        String transportToNext,
        @JsonProperty("estimated_cost")
        String estimatedCost,
        @JsonProperty("highlighted")
        Boolean highlighted) {
}
