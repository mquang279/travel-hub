package edu.uet.travel_hub.infrastructure.persistence.mapper;

import edu.uet.travel_hub.domain.model.UserModel;
import edu.uet.travel_hub.infrastructure.persistence.entity.UserJpaEntity;

public class UserPersistenceMapper {
    public UserJpaEntity toEntity(UserModel user) {
        return UserJpaEntity.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .hashPassword(user.getPassword()).build();
    }

    public UserModel toDomain(UserJpaEntity user) {
        return new UserModel(null, null, null, null);
    }
}
