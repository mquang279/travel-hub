package edu.uet.travel_hub.infrastructure.persistence.repository.jpa;

import edu.uet.travel_hub.infrastructure.persistence.entity.FollowEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

@Repository
public interface FollowJpaRepository extends JpaRepository<FollowEntity, Long> {
    @EntityGraph(attributePaths = "follower")
    Page<FollowEntity> findByFollowingId(Long followingId, Pageable pageable);

    @EntityGraph(attributePaths = "following")
    Page<FollowEntity> findByFollowerId(Long followerId, Pageable pageable);

    Optional<FollowEntity> findByFollowerIdAndFollowingId(Long followerId, Long followingId);

    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    @Query("""
            SELECT f.following.id FROM FollowEntity f
            WHERE f.follower.id = :followerId
              AND f.following.id IN :followingIds
            """)
    Set<Long> findFollowingIdsByFollowerIdAndFollowingIdIn(
            @Param("followerId") Long followerId,
            @Param("followingIds") Collection<Long> followingIds);
}
