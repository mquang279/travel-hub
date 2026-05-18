package edu.uet.travel_hub.application.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

import edu.uet.travel_hub.domain.enums.TripStatus;

public record TripInfoResponse(
        Long id,
        String name,
        String location,
        String coverImageUrl,
        String description,
        @JsonFormat(pattern = "dd/MM/yyyy")
        LocalDate startDate,
        @JsonFormat(pattern = "dd/MM/yyyy")
        LocalDate endDate,
        BigDecimal budgetMin,
        BigDecimal budgetMax,
        TripStatus status,
        String inviteCode,
        Integer maxMembers) {
}