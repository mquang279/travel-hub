package edu.uet.travel_hub.infrastructure.persistence.repository.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import edu.uet.travel_hub.application.dto.response.PaginationResponse;
import edu.uet.travel_hub.application.port.out.CommentRepository;
import edu.uet.travel_hub.domain.model.CommentModel;
import edu.uet.travel_hub.infrastructure.persistence.entity.CommentEntity;
import edu.uet.travel_hub.infrastructure.persistence.mapper.CommentPersistenceMapper;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.CommentJpaRepository;

@Repository
public class CommentRepositoryImpl implements CommentRepository {
    private final CommentJpaRepository commentJpaRepository;
    private final CommentPersistenceMapper mapper;

    public CommentRepositoryImpl(CommentJpaRepository commentJpaRepository, CommentPersistenceMapper mapper) {
        this.commentJpaRepository = commentJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public CommentModel save(CommentModel comment) {
        CommentEntity entity = this.mapper.toEntity(comment);
        return this.mapper.toModel(this.commentJpaRepository.save(entity));
    }

    @Override
    public void delete(Long commentId) {
        CommentEntity entity = this.commentJpaRepository.findById(commentId).get();
        this.commentJpaRepository.delete(entity);
    }

    @Override
    public CommentModel findById(Long commentId) {
        CommentEntity entity = this.commentJpaRepository.findById(commentId).get();
        return this.mapper.toModel(entity);
    }

    @Override
    public PaginationResponse<CommentModel> findByPostId(int page, int pageSize, Long postId) {
        PageRequest request = PageRequest.of(page, pageSize);
        Page<CommentEntity> comments = this.commentJpaRepository.findByPostId(postId, request);
        return new PaginationResponse<>(
                comments.getNumber(),
                comments.getSize(),
                comments.getTotalPages(),
                comments.getTotalElements(),
                comments.stream().map(mapper::toModel).toList());
    }

}
