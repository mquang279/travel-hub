package edu.uet.travel_hub.application.dto.response;

import java.time.LocalDateTime;

public record TripInviteCodeResponse(
        String inviteCode,
        String inviteLink,
        LocalDateTime expiredAt) {
}
