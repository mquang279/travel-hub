package edu.uet.travel_hub.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;

import edu.uet.travel_hub.domain.model.CommentModel;
import edu.uet.travel_hub.infrastructure.persistence.entity.CommentEntity;

@Component
public class CommentPersistenceMapper {
    private final UserPersistenceMapper userMapper;
    private final PostPersistenceMapper postMapper;

    public CommentPersistenceMapper(UserPersistenceMapper userMapper, PostPersistenceMapper postMapper) {
        this.userMapper = userMapper;
        this.postMapper = postMapper;
    }

    public CommentModel toModel(CommentEntity entity) {
        return CommentModel.builder()
                .id(entity.getId())
                .content(entity.getContent())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .post(postMapper.toDomain(entity.getPost()))
                .owner(userMapper.toDomain(entity.getUser())).build();
    }

    public CommentEntity toEntity(CommentModel model) {
        return CommentEntity.builder()
                .content(model.getContent())
                .post(postMapper.toEntity(model.getPost()))
                .user(userMapper.toEntity(model.getOwner()))
                .build();
    }
}
