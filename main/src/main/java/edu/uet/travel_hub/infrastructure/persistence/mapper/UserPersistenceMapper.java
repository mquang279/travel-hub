package edu.uet.travel_hub.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;

import edu.uet.travel_hub.domain.model.UserModel;
import edu.uet.travel_hub.infrastructure.persistence.entity.UserEntity;

@Component
public class UserPersistenceMapper {
    public UserEntity toEntity(UserModel user) {
        return UserEntity.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .hashPassword(user.getPassword())
                .role(user.getRole())
                .build();
    }

    public UserModel toDomain(UserEntity user) {
        return new UserModel(user.getId(), user.getEmail(), user.getUsername(), user.getHashPassword(), user.getRole());
    }
}
