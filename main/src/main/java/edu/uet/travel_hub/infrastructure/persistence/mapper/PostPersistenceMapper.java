package edu.uet.travel_hub.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;

import edu.uet.travel_hub.domain.model.PostModel;
import edu.uet.travel_hub.infrastructure.persistence.entity.PostJpaEntity;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.UserJpaRepository;

@Component
public class PostPersistenceMapper {
    private final UserJpaRepository userJpaRepository;

    public PostPersistenceMapper(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    public PostJpaEntity toEntity(PostModel model) {
        if (model.getUserId() == null) {
            throw new IllegalArgumentException("Post userId must not be null");
        }

        return PostJpaEntity.builder()
                .description(model.getDescription())
                .imageUrl(model.getImageUrl())
                .location(model.getLocation())
                .user(this.userJpaRepository.findById(model.getUserId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "User not found with id: " + model.getUserId())))
                .build();
    }

    public PostModel toDomain(PostJpaEntity entity) {
        return PostModel.builder()
                .description(entity.getDescription())
                .imageUrl(entity.getImageUrl())
                .id(entity.getId())
                .location(entity.getLocation())
                .userId(entity.getUser().getId()).build();
    }
}
