package edu.uet.travel_hub.domain.model;

import java.time.Instant;

import lombok.Builder;

@Builder
public record ItineraryDetailRowModel(
        Long itineraryId,
        String groupName,
        int version,
        Long ownerId,
        Instant createdAt,
        Instant updatedAt,
        Long dayId,
        Integer dayIndex,
        String dayLabel,
        String dateLabel,
        Long stopId,
        Integer sortOrder,
        String startTime,
        String endTime,
        String title,
        String placeName,
        String note,
        String transportToNext,
        String estimatedCost,
        Long colorHex,
        String iconName) {
}
