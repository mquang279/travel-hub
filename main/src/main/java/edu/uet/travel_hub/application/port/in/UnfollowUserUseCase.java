package edu.uet.travel_hub.application.port.in;

public interface UnfollowUserUseCase {
    void unfollowUser(Long currentUserId, Long targetUserId);
}
