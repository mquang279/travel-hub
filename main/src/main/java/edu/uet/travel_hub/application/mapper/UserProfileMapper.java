package edu.uet.travel_hub.application.mapper;

import org.springframework.stereotype.Component;

import edu.uet.travel_hub.domain.dto.response.UserFollowResponse;
import edu.uet.travel_hub.domain.dto.response.UserProfileResponse;
import edu.uet.travel_hub.domain.model.UserModel;

@Component
public class UserProfileMapper {
    public UserProfileResponse toProfileResponse(UserModel user, boolean isFollowing) {
        String displayName = resolveDisplayName(user);
        return UserProfileResponse.builder()
                .id(user.getId())
                .avatarUrl(user.getAvatarUrl())
                .name(displayName)
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

    public UserFollowResponse toFollowResponse(UserModel user, boolean following) {
        String displayName = resolveDisplayName(user);
        return UserFollowResponse.builder()
                .id(user.getId())
                .name(displayName)
                .username(user.getUsername())
                .avatarUrl(user.getAvatarUrl())
                .following(following)
                .build();
    }

    private String resolveDisplayName(UserModel user) {
        if (user.getName() != null && !user.getName().isBlank()) {
            return user.getName();
        }
        return user.getUsername();
    }
}
