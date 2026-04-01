package edu.uet.travel_hub.infrastructure.persistence.repository;

import edu.uet.travel_hub.infrastructure.persistence.entity.FollowEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaFollowRepository extends JpaRepository<FollowEntity, Long> {
    Page<FollowEntity> findByFollowingId(Long followingId, Pageable pageable);
    Page<FollowEntity> findByFollowerId(Long followerId, Pageable pageable);
    Optional<FollowEntity> findByFollowerIdAndFollowingId(Long followerId, Long followingId);
    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);
}
