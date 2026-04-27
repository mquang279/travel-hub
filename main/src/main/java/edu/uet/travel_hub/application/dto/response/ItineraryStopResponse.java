package edu.uet.travel_hub.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ItineraryStopResponse(
        Long id,
        @JsonProperty("sort_order")
        int sortOrder,
        @JsonProperty("start_time")
        String startTime,
        @JsonProperty("end_time")
        String endTime,
        String title,
        @JsonProperty("place_name")
        String placeName,
        String note,
        @JsonProperty("transport_to_next")
        String transportToNext,
        @JsonProperty("estimated_cost")
        String estimatedCost,
        boolean highlighted) {
}
