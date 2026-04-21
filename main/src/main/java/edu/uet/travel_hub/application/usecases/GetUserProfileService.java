package edu.uet.travel_hub.application.usecases;

import org.springframework.stereotype.Service;

import edu.uet.travel_hub.application.exception.ResourceNotFoundException;
import edu.uet.travel_hub.application.mapper.UserProfileMapper;
import edu.uet.travel_hub.application.port.in.GetUserProfileUseCase;
import edu.uet.travel_hub.application.port.out.FollowRepository;
import edu.uet.travel_hub.application.port.out.UserRepository;
import edu.uet.travel_hub.domain.dto.response.UserProfileResponse;
import edu.uet.travel_hub.domain.model.UserModel;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetUserProfileService implements GetUserProfileUseCase {
    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final UserProfileMapper userProfileMapper;

    @Override
    public UserProfileResponse getProfile(Long currentUserId, Long targetUserId) {
        UserModel user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean isFollowing = currentUserId != null
                && !currentUserId.equals(targetUserId)
                && followRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId);

        return userProfileMapper.toProfileResponse(user, isFollowing);
    }
}
