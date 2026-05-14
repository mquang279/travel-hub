package edu.uet.travel_hub.application.dto.response;

public record TripMemberResponse(
        Long userId,
        String name,
        String avatarUrl,
        String role) {
}