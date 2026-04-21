package edu.uet.travel_hub.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;

import edu.uet.travel_hub.domain.model.PostModel;
import edu.uet.travel_hub.infrastructure.persistence.entity.PostEntity;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.UserJpaRepository;

@Component
public class PostPersistenceMapper {
    private final UserJpaRepository userJpaRepository;

    public PostPersistenceMapper(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    public PostEntity toEntity(PostModel model) {
        if (model.getUserId() == null) {
            throw new IllegalArgumentException("Post userId must not be null");
        }

        return PostEntity.builder()
                .description(model.getDescription())
                .imageUrls(model.getImageUrls())
                .location(model.getLocation())
                .user(this.userJpaRepository.findById(model.getUserId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "User not found with id: " + model.getUserId())))
                .build();
    }

    public PostModel toDomain(PostEntity entity) {
        return PostModel.builder()
                .description(entity.getDescription())
                .imageUrls(entity.getImageUrls())
                .id(entity.getId())
                .location(entity.getLocation())
                .userId(entity.getUser().getId()).build();
    }
}
