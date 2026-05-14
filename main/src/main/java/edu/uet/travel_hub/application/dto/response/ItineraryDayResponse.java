package edu.uet.travel_hub.application.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ItineraryDayResponse(
        Long id,
        @JsonProperty("day_index")
        int dayIndex,
        String label,
        @JsonProperty("date_label")
        String dateLabel,
        List<ItineraryStopResponse> stops) {
}
