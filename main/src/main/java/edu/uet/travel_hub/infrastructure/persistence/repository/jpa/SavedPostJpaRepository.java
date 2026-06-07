package edu.uet.travel_hub.infrastructure.persistence.repository.jpa;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import edu.uet.travel_hub.infrastructure.persistence.entity.SavedPostEntity;

@Repository
public interface SavedPostJpaRepository extends JpaRepository<SavedPostEntity, Long> {
    Optional<SavedPostEntity> findByUserIdAndPostId(Long userId, Long postId);

    boolean existsByUserIdAndPostId(Long userId, Long postId);

    long countByPostId(Long postId);

    @Query("SELECT s.post.id FROM SavedPostEntity s WHERE s.user.id = :userId AND s.post.id IN :postIds")
    List<Long> findSavedPostIds(@Param("userId") Long userId, @Param("postIds") Collection<Long> postIds);
}
