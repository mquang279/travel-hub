package edu.uet.travel_hub.application.port.in;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import edu.uet.travel_hub.domain.dto.response.UserFollowResponse;

public interface GetFollowingUseCase {
    Page<UserFollowResponse> getFollowing(Long currentUserId, Long targetUserId, Pageable pageable);
}
