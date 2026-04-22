package edu.uet.travel_hub.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;

import edu.uet.travel_hub.domain.model.UserModel;
import edu.uet.travel_hub.infrastructure.persistence.entity.UserEntity;

@Component
public class UserPersistenceMapper {
    public UserEntity toEntity(UserModel user) {
        return UserEntity.builder()
                .id(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .email(user.getEmail())
                .bio(user.getBio())
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(user.getAvatarUrl())
                .dateOfBirth(user.getDateOfBirth())
                .gender(user.getGender())
                .location(user.getLocation())
                .followersCount(user.getFollowersCount())
                .followingCount(user.getFollowingCount())
                .postsCount(user.getPostsCount())
                .build();
    }

    public UserModel toDomain(UserEntity user) {
        return UserModel.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .name(user.getName())
                .bio(user.getBio())
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(user.getAvatarUrl())
                .dateOfBirth(user.getDateOfBirth())
                .gender(user.getGender())
                .location(user.getLocation())
                .followersCount(user.getFollowersCount())
                .followingCount(user.getFollowingCount())
                .postsCount(user.getPostsCount())
                .build();
    }
}
