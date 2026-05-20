package edu.uet.travel_hub.application.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record CreateItineraryStopRequest(
                @JsonProperty("day_id") Long dayId,
                @JsonProperty("day_index") Integer dayIndex,
                @JsonProperty("day_label") String dayLabel,
                @JsonProperty("day_date_label") String dayDateLabel,
                @JsonProperty("sort_order") Integer sortOrder,
                @JsonProperty("start_time") String startTime,
                @JsonProperty("end_time") String endTime,
                @NotBlank(message = "title is required") String title,
                @NotBlank(message = "placeName is required") @JsonProperty("place_name") String placeName,
                String note,
                @JsonProperty("transport_to_next") String transportToNext,
                @JsonProperty("estimated_cost") String estimatedCost,
                @JsonProperty("color_hex") Long colorHex,
                @JsonProperty("icon_name") String iconName) {
}
