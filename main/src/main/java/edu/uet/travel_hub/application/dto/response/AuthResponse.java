package edu.uet.travel_hub.application.dto.response;

public record AuthResponse(
    String accessToken, 
    String refreshToken,
    Long userId) {
} 
