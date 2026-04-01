package edu.uet.travel_hub.application.usecases;

import edu.uet.travel_hub.domain.dto.response.UserFollowResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FollowUseCase {
    Page<UserFollowResponse> getFollowers(Long currentUserId, Long targetUserId, Pageable pageable);
    Page<UserFollowResponse> getFollowing(Long currentUserId, Long targetUserId, Pageable pageable);
    void followUser(Long currentUserId, Long targetUserId);
    void unfollowUser(Long currentUserId, Long targetUserId);
}
