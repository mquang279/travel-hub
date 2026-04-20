package edu.uet.travel_hub.application.port.in;

public interface FollowUserUseCase {
    void followUser(Long currentUserId, Long targetUserId);
}
