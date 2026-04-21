package edu.uet.travel_hub.infrastructure.persistence.repository.jpa;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.jpa.repository.JpaRepository;

import edu.uet.travel_hub.infrastructure.persistence.entity.PostEntity;

public interface PostJpaRepository extends JpaRepository<PostEntity, Long> {
    @Modifying
    @Query("UPDATE PostEntity p SET p.likeCount = p.likeCount + 1 WHERE p.id = :id")
    void incrementLike(Long id);
}
