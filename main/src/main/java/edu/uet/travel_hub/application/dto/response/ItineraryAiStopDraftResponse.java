package edu.uet.travel_hub.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ItineraryAiStopDraftResponse(
        Long id,
        @JsonProperty("day_id")
        Long dayId,
        @JsonProperty("day_index")
        Integer dayIndex,
        @JsonProperty("sort_order")
        Integer sortOrder,
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
        @JsonProperty("color_hex")
        Long colorHex,
        @JsonProperty("icon_name")
        String iconName) {
}
