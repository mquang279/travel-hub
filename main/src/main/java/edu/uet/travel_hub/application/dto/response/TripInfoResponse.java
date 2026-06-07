package edu.uet.travel_hub.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import edu.uet.travel_hub.domain.enums.TripStatus;

public record TripInfoResponse(
        Long id,
        String name,
        String location,
        String coverImageUrl,
        Long placeId,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal budgetMin,
        BigDecimal budgetMax,
        TripStatus status,
        String inviteCode,
        Integer maxMembers,
        List<String> imageUrls) {
}
