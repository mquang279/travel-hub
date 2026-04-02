package edu.uet.travel_hub.infrastructure.persistence.mapper;

import edu.uet.travel_hub.domain.model.PostModel;
import edu.uet.travel_hub.infrastructure.persistence.entity.PostJpaEntity;

public class PostPersistenceMapper {
    public PostJpaEntity toEntity(PostModel model) {
        return PostJpaEntity.builder()
                .description(model.getDescription())
                .imageUrl(model.getImageUrl()).build();
    }

    public PostModel toDomain(PostJpaEntity entity) {
        return PostModel.builder()
                .description(entity.getDescription())
                .imageUrl(entity.getImageUrl())
                .id(entity.getId()).build();
    }
}
