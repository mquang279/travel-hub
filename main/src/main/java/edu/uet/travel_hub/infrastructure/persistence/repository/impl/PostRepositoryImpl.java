package edu.uet.travel_hub.infrastructure.persistence.repository.impl;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import edu.uet.travel_hub.application.dto.response.PaginationResponse;
import edu.uet.travel_hub.application.port.out.PostRepository;
import edu.uet.travel_hub.domain.model.PostModel;
import edu.uet.travel_hub.infrastructure.persistence.entity.PostEntity;
import edu.uet.travel_hub.infrastructure.persistence.mapper.PostPersistenceMapper;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.PostJpaRepository;
import jakarta.transaction.Transactional;

@Component
public class PostRepositoryImpl implements PostRepository {
    private final PostJpaRepository postJpaRepository;
    private final PostPersistenceMapper mapper;

    public PostRepositoryImpl(PostJpaRepository postJpaRepository, PostPersistenceMapper mapper) {
        this.postJpaRepository = postJpaRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public PostModel save(Long userId, PostModel post) {
        if (userId == null) {
            throw new IllegalArgumentException("Current user id must not be null");
        }

        post.setUserId(userId);
        PostEntity entity = mapper.toEntity(post);
        PostEntity savedEntity = this.postJpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<PostModel> findById(Long id) {
        return this.postJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public PaginationResponse<PostModel> getAll(int pageNumber, int pageSize) {
        PageRequest request = PageRequest.of(pageNumber, pageSize);
        Page<PostEntity> posts = this.postJpaRepository.findAll(request);
        PaginationResponse<PostModel> response = new PaginationResponse<PostModel>(
                posts.getNumber(),
                posts.getSize(),
                posts.getTotalPages(),
                posts.getTotalElements(),
                posts.getContent().stream().map(mapper::toDomain).toList());
        return response;
    }

    @Override
    @Transactional
    public void increaseLikeCount(Long id) {
        this.postJpaRepository.incrementLike(id);
    }

    @Override
    @Transactional
    public void decreaseLikeCount(Long id) {
        this.postJpaRepository.decrementLike(id);
    }

    @Override
    public int getLikeCount(Long id) {
        PostEntity postEntity = this.postJpaRepository.findById(id).get();
        return postEntity.getLikeCount();
    }

}
