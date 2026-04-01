package edu.uet.travel_hub.infrastructure.security;

import edu.uet.travel_hub.domain.model.UserModel;

public record UserClaims(Long id, String email, String username) {
    public static UserClaims from(UserModel user) {
        return new UserClaims(
                user.getId(),
                user.getEmail(),
                user.getUsername());
    }
}
