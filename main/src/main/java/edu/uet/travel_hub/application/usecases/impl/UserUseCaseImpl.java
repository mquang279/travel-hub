package edu.uet.travel_hub.application.usecases.impl;

import edu.uet.travel_hub.application.exception.ResourceNotFoundException;
import edu.uet.travel_hub.application.port.in.UserUseCase;
import edu.uet.travel_hub.domain.dto.request.UpdateProfileRequest;
import edu.uet.travel_hub.domain.dto.response.UserProfileResponse;
import edu.uet.travel_hub.infrastructure.persistence.entity.UserEntity;
import edu.uet.travel_hub.infrastructure.persistence.repository.JpaFollowRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.JpaUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserUseCaseImpl implements UserUseCase {

    private final JpaUserRepository userRepository;
    private final JpaFollowRepository followRepository;

    @Override
    public UserProfileResponse getProfile(Long currentUserId, Long targetUserId) {
        UserEntity user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean isFollowing = currentUserId != null
                && !currentUserId.equals(targetUserId)
                && followRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId);

        return mapToResponse(user, isFollowing);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getName() != null) user.setName(request.getName());
        if (request.getUsername() != null) user.setUsername(request.getUsername());
        if (request.getBio() != null) user.setBio(request.getBio());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());
        if (request.getDateOfBirth() != null) user.setDateOfBirth(request.getDateOfBirth());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());
        if (request.getLocation() != null) user.setLocation(request.getLocation());

        userRepository.save(user);
        return mapToResponse(user, false);
    }

    @Override
    @Transactional
    public String uploadAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Avatar file must not be empty");
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String uploadedUrl = "https://example.com/avatars/" + file.getOriginalFilename();
        
        user.setAvatarUrl(uploadedUrl);
        userRepository.save(user);
        
        return uploadedUrl;
    }

    private UserProfileResponse mapToResponse(UserEntity user, boolean isFollowing) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .avatarUrl(user.getAvatarUrl())
                .name(user.getName())
                .username(user.getUsername())
                .bio(user.getBio())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .dateOfBirth(user.getDateOfBirth())
                .gender(user.getGender())
                .location(user.getLocation())
                .followersCount(user.getFollowersCount())
                .followingCount(user.getFollowingCount())
                .postsCount(user.getPostsCount())
                .isFollowing(isFollowing)
                .build();
    }
}
