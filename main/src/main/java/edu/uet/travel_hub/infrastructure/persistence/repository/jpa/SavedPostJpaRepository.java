package edu.uet.travel_hub.infrastructure.persistence.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.uet.travel_hub.infrastructure.persistence.entity.SavedPostEntity;

@Repository
public interface SavedPostJpaRepository extends JpaRepository<SavedPostEntity, Long> {
    boolean existsByUserIdAndPostId(Long userId, Long postId);
}
