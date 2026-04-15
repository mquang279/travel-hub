package edu.uet.travel_hub.application.usecases;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import edu.uet.travel_hub.application.mapper.UserProfileMapper;
import edu.uet.travel_hub.application.port.in.GetFollowersUseCase;
import edu.uet.travel_hub.application.port.out.FollowRepository;
import edu.uet.travel_hub.domain.dto.response.UserFollowResponse;
import edu.uet.travel_hub.domain.model.UserModel;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetFollowersService implements GetFollowersUseCase {
    private final FollowRepository followRepository;
    private final UserProfileMapper userProfileMapper;

    @Override
    public Page<UserFollowResponse> getFollowers(Long currentUserId, Long targetUserId, Pageable pageable) {
        return followRepository.findFollowers(targetUserId, pageable)
                .map(follower -> mapFollower(currentUserId, follower));
    }

    private UserFollowResponse mapFollower(Long currentUserId, UserModel follower) {
        boolean following = currentUserId != null
                && followRepository.existsByFollowerIdAndFollowingId(currentUserId, follower.getId());
        return userProfileMapper.toFollowResponse(follower, following);
    }
}
