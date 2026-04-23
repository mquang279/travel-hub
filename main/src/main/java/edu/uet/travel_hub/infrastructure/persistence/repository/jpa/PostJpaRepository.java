package edu.uet.travel_hub.infrastructure.persistence.repository.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.uet.travel_hub.infrastructure.persistence.entity.PostEntity;

public interface PostJpaRepository extends JpaRepository<PostEntity, Long> {
    @Query("SELECT p FROM PostEntity p WHERE p.user.id = :userId ORDER BY p.createdAt DESC, p.id DESC")
    Page<PostEntity> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE PostEntity p SET p.likeCount = p.likeCount + 1 WHERE p.id = :postId")
    void incrementLike(@Param("postId") Long postId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE PostEntity p SET p.likeCount = p.likeCount - 1 WHERE p.id = :postId")
    void decrementLike(@Param("postId") Long postId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE PostEntity p SET p.commentCount = p.commentCount + 1 WHERE p.id = :postId")
    void incrementComment(@Param("postId") Long postId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE PostEntity p SET p.commentCount = p.commentCount - 1 WHERE p.id = :postId")
    void decrementComment(@Param("postId") Long postId);
}
