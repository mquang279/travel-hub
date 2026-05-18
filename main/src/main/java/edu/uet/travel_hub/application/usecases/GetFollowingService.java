package edu.uet.travel_hub.application.usecases;

import java.util.Set;
import java.util.stream.Collectors;

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
        Page<UserModel> followingUsers = followRepository.findFollowing(targetUserId, pageable);
        Set<Long> followingIds = findFollowingIds(currentUserId, followingUsers);

        return followingUsers.map(followingUser -> userProfileMapper.toFollowResponse(
                followingUser,
                followingIds.contains(followingUser.getId())));
    }

    private Set<Long> findFollowingIds(Long currentUserId, Page<UserModel> users) {
        if (currentUserId == null || users.isEmpty()) {
            return Set.of();
        }
        Set<Long> userIds = users.stream()
                .map(UserModel::getId)
                .collect(Collectors.toSet());
        return followRepository.findFollowingIds(currentUserId, userIds);
    }
}
