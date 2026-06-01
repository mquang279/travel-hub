package edu.uet.travel_hub.application.dto.request;

public record RefreshTokenRequest(String refreshToken) {
    @Override
    public String toString() {
        return "RefreshTokenRequest[refreshToken=***]";
    }
}
