package edu.uet.travel_hub.infrastructure.persistence.mapper;

import edu.uet.travel_hub.domain.model.UserModel;
import edu.uet.travel_hub.infrastructure.persistence.entity.UserJpaEntity;

public class UserPersistenceMapper {
    public UserJpaEntity toEntity(UserModel user) {
        return new UserJpaEntity();
    }

    public UserModel toDomain(UserJpaEntity user) {
        return new UserModel(null, null, null);
    }
}
