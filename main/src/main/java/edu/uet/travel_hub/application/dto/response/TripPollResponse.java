package edu.uet.travel_hub.application.dto.response;

import java.util.List;

import edu.uet.travel_hub.domain.enums.TripPollCategory;

public record TripPollResponse(
        Long id,
        String title,
        TripPollCategory category,
        int votesCount,
        boolean isWinning,
        boolean hasVoted,
        List<String> voters) {
}