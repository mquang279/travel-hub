package edu.uet.travel_hub.application.dto.response;

public record TopTravelerResponse(
        Long id,
        String username,
        String name,
        String avatarUrl,
        long score,
        boolean following,
        boolean currentUser) {
}
