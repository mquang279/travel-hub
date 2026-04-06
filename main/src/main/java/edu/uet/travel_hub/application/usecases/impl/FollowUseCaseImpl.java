package edu.uet.travel_hub.application.usecases.impl;

import edu.uet.travel_hub.application.exception.ResourceNotFoundException;
import edu.uet.travel_hub.application.port.in.FollowUseCase;
import edu.uet.travel_hub.domain.dto.response.UserFollowResponse;
import edu.uet.travel_hub.infrastructure.persistence.entity.FollowEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.UserEntity;
import edu.uet.travel_hub.infrastructure.persistence.repository.JpaFollowRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.JpaUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FollowUseCaseImpl implements FollowUseCase {

    private final JpaFollowRepository followRepository;
    private final JpaUserRepository userRepository;

    @Override
    public Page<UserFollowResponse> getFollowers(Long currentUserId, Long targetUserId, Pageable pageable) {
        Page<FollowEntity> follows = followRepository.findByFollowingId(targetUserId, pageable);
        return follows.map(f -> {
            UserEntity follower = f.getFollower();
            boolean following = currentUserId != null && 
                followRepository.existsByFollowerIdAndFollowingId(currentUserId, follower.getId());
            return mapToResponse(follower, following);
        });
    }

    @Override
    public Page<UserFollowResponse> getFollowing(Long currentUserId, Long targetUserId, Pageable pageable) {
        Page<FollowEntity> follows = followRepository.findByFollowerId(targetUserId, pageable);
        return follows.map(f -> {
            UserEntity following = f.getFollowing();
            boolean followingByMe = currentUserId != null && 
                followRepository.existsByFollowerIdAndFollowingId(currentUserId, following.getId());
            return mapToResponse(following, followingByMe);
        });
    }

    @Override
    @Transactional
    public void followUser(Long currentUserId, Long targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            throw new IllegalArgumentException("Cannot follow yourself");
        }

        UserEntity currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
        UserEntity targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Target user not found"));

        Optional<FollowEntity> existingFollow = followRepository.findByFollowerIdAndFollowingId(currentUserId, targetUserId);

        if (existingFollow.isEmpty()) {
            FollowEntity newFollow = FollowEntity.builder()
                    .follower(currentUser)
                    .following(targetUser)
                    .build();
            followRepository.save(newFollow);

            userRepository.incrementFollowing(currentUserId);
            userRepository.incrementFollowers(targetUserId);

            currentUser.setFollowingCount(currentUser.getFollowingCount() + 1);
            targetUser.setFollowersCount(targetUser.getFollowersCount() + 1);
        }
    }

    @Override
    @Transactional
    public void unfollowUser(Long currentUserId, Long targetUserId) {
        UserEntity currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
        UserEntity targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Target user not found"));

        Optional<FollowEntity> existingFollow = followRepository.findByFollowerIdAndFollowingId(currentUserId, targetUserId);

        if (existingFollow.isPresent()) {
            followRepository.delete(existingFollow.get());

            userRepository.decrementFollowing(currentUserId);
            userRepository.decrementFollowers(targetUserId);

            currentUser.setFollowingCount(Math.max(0, currentUser.getFollowingCount() - 1));
            targetUser.setFollowersCount(Math.max(0, targetUser.getFollowersCount() - 1));
        }
    }

    private UserFollowResponse mapToResponse(UserEntity user, boolean following) {
        return UserFollowResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .username(user.getUsername())
                .avatarUrl(user.getAvatarUrl())
                .following(following)
                .build();
    }
}
