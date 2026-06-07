package edu.uet.travel_hub.infrastructure.security;

import edu.uet.travel_hub.domain.enums.Role;

public record FirebaseAuthenticatedUser(
        Long id,
        String firebaseUid,
        String email,
        String username,
        Role role) {
}
