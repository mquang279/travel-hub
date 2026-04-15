package edu.uet.travel_hub.application.usecases;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import edu.uet.travel_hub.application.mapper.UserProfileMapper;
import edu.uet.travel_hub.application.port.in.GetFollowingUseCase;
import edu.uet.travel_hub.application.port.out.FollowRepository;
import edu.uet.travel_hub.domain.dto.response.UserFollowResponse;
import edu.uet.travel_hub.domain.model.UserModel;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetFollowingService implements GetFollowingUseCase {
    private final FollowRepository followRepository;
    private final UserProfileMapper userProfileMapper;

    @Override
    public Page<UserFollowResponse> getFollowing(Long currentUserId, Long targetUserId, Pageable pageable) {
        return followRepository.findFollowing(targetUserId, pageable)
                .map(followingUser -> mapFollowing(currentUserId, followingUser));
    }

    private UserFollowResponse mapFollowing(Long currentUserId, UserModel followingUser) {
        boolean followingByMe = currentUserId != null
                && followRepository.existsByFollowerIdAndFollowingId(currentUserId, followingUser.getId());
        return userProfileMapper.toFollowResponse(followingUser, followingByMe);
    }
}
