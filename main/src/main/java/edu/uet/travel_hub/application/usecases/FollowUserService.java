package edu.uet.travel_hub.application.usecases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.uet.travel_hub.application.exception.ResourceNotFoundException;
import edu.uet.travel_hub.application.port.in.FollowUserUseCase;
import edu.uet.travel_hub.application.port.out.FollowRepository;
import edu.uet.travel_hub.application.port.out.UserRepository;
import edu.uet.travel_hub.domain.model.FollowModel;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FollowUserService implements FollowUserUseCase {
    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void followUser(Long currentUserId, Long targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            throw new IllegalArgumentException("Cannot follow yourself");
        }

        if (userRepository.findById(currentUserId).isEmpty()) {
            throw new ResourceNotFoundException("Current user not found");
        }
        if (userRepository.findById(targetUserId).isEmpty()) {
            throw new ResourceNotFoundException("Target user not found");
        }

        boolean alreadyFollowed = followRepository.findByFollowerIdAndFollowingId(currentUserId, targetUserId)
                .isPresent();

        if (!alreadyFollowed) {
            followRepository.save(new FollowModel(null, currentUserId, targetUserId));
            userRepository.incrementFollowing(currentUserId);
            userRepository.incrementFollowers(targetUserId);
        }
    }
}
