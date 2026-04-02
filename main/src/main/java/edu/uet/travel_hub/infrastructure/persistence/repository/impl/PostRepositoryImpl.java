package edu.uet.travel_hub.infrastructure.persistence.repository.impl;

import java.util.Optional;

import edu.uet.travel_hub.application.port.out.PostRepository;
import edu.uet.travel_hub.domain.model.PostModel;
import edu.uet.travel_hub.infrastructure.persistence.entity.PostJpaEntity;
import edu.uet.travel_hub.infrastructure.persistence.mapper.PostPersistenceMapper;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.PostJpaRepository;

public class PostRepositoryImpl implements PostRepository {
    private final PostJpaRepository postJpaRepository;
    private final PostPersistenceMapper mapper;

    public PostRepositoryImpl(PostJpaRepository postJpaRepository, PostPersistenceMapper mapper) {
        this.postJpaRepository = postJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public PostModel save(Long userId, PostModel post) {
        PostJpaEntity entity = mapper.toEntity(post);
        PostJpaEntity savedEntity = this.postJpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<PostModel> findById(Long id) {
        return this.postJpaRepository.findById(id).map(mapper::toDomain);
    }

}
