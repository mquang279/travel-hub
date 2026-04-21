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
                .hashPassword(user.getPassword())
                .role(user.getRole())
                .bio(user.getBio())
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(user.getAvatarUrl())
                .dateOfBirth(user.getDateOfBirth())
                .gender(user.getGender())
                .location(user.getLocation())
                .followersCount(user.getFollowersCount())
                .followingCount(user.getFollowingCount())
                .postsCount(user.getPostsCount())
                .refreshToken(user.getRefreshToken())
                .build();
    }

    public UserModel toDomain(UserEntity user) {
        UserModel model = new UserModel(user.getId(), user.getEmail(), user.getUsername(), user.getHashPassword(),
                user.getRole());
        model.setName(user.getName());
        model.setBio(user.getBio());
        model.setPhoneNumber(user.getPhoneNumber());
        model.setAvatarUrl(user.getAvatarUrl());
        model.setDateOfBirth(user.getDateOfBirth());
        model.setGender(user.getGender());
        model.setLocation(user.getLocation());
        model.setFollowersCount(user.getFollowersCount());
        model.setFollowingCount(user.getFollowingCount());
        model.setPostsCount(user.getPostsCount());
        model.setRefreshToken(user.getRefreshToken());
        return model;
    }
}
