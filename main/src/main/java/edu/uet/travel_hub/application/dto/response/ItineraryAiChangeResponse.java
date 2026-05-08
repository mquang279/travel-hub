package edu.uet.travel_hub.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ItineraryAiChangeResponse(
        @JsonProperty("change_id")
        String changeId,
        String type,
        String reason,
        @JsonProperty("insert_at")
        Integer insertAt,
        @JsonProperty("from_day_id")
        Long fromDayId,
        @JsonProperty("from_day_index")
        Integer fromDayIndex,
        @JsonProperty("from_index")
        Integer fromIndex,
        @JsonProperty("to_day_id")
        Long toDayId,
        @JsonProperty("to_day_index")
        Integer toDayIndex,
        @JsonProperty("to_index")
        Integer toIndex,
        @JsonProperty("target_day_id")
        Long targetDayId,
        @JsonProperty("target_stop_id")
        Long targetStopId,
        @JsonProperty("day_before")
        ItineraryAiDayDraftResponse dayBefore,
        @JsonProperty("day_after")
        ItineraryAiDayDraftResponse dayAfter,
        @JsonProperty("stop_before")
        ItineraryAiStopDraftResponse stopBefore,
        @JsonProperty("stop_after")
        ItineraryAiStopDraftResponse stopAfter) {
}
