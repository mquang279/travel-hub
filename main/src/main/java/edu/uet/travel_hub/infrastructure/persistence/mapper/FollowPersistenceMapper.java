package edu.uet.travel_hub.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;

import edu.uet.travel_hub.domain.model.FollowModel;
import edu.uet.travel_hub.infrastructure.persistence.entity.FollowEntity;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.UserJpaRepository;

@Component
public class FollowPersistenceMapper {
    private final UserJpaRepository userJpaRepository;

    public FollowPersistenceMapper(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    public FollowModel toDomain(FollowEntity entity) {
        return new FollowModel(
                entity.getId(),
                entity.getFollower().getId(),
                entity.getFollowing().getId());
    }

    public FollowEntity toEntity(FollowModel model) {
        return FollowEntity.builder()
                .id(model.getId())
                .follower(this.userJpaRepository.findById(model.getFollowerId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Follower not found with id: " + model.getFollowerId())))
                .following(this.userJpaRepository.findById(model.getFollowingId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Following user not found with id: " + model.getFollowingId())))
                .build();
    }
}
