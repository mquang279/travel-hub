package edu.uet.travel_hub.infrastructure.persistence.repository.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.uet.travel_hub.infrastructure.persistence.entity.CommentEntity;

@Repository
public interface CommentJpaRepository extends JpaRepository<CommentEntity, Long> {
    Page<CommentEntity> findByPostId(Long postId, Pageable pageable);
}
