package edu.uet.travel_hub.application.usecases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.uet.travel_hub.application.exception.ResourceNotFoundException;
import edu.uet.travel_hub.application.port.in.UnfollowUserUseCase;
import edu.uet.travel_hub.application.port.out.FollowRepository;
import edu.uet.travel_hub.application.port.out.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UnfollowUserService implements UnfollowUserUseCase {
    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void unfollowUser(Long currentUserId, Long targetUserId) {
        if (userRepository.findById(currentUserId).isEmpty()) {
            throw new ResourceNotFoundException("Current user not found");
        }
        if (userRepository.findById(targetUserId).isEmpty()) {
            throw new ResourceNotFoundException("Target user not found");
        }

        boolean existingFollow = followRepository.findByFollowerIdAndFollowingId(currentUserId, targetUserId)
                .isPresent();

        if (existingFollow) {
            followRepository.deleteByFollowerIdAndFollowingId(currentUserId, targetUserId);
            userRepository.decrementFollowing(currentUserId);
            userRepository.decrementFollowers(targetUserId);
        }
    }
}
