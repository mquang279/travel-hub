package edu.uet.travel_hub.infrastructure.persistence.repository.jpa;

import edu.uet.travel_hub.infrastructure.persistence.entity.UserEntity;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {

    @Modifying
    @Transactional
    @Query("update UserEntity u set u.followingCount = u.followingCount + 1 where u.id = :id")
    void incrementFollowing(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("update UserEntity u set u.followingCount = CASE WHEN u.followingCount > 0 THEN u.followingCount - 1 ELSE 0 END where u.id = :id")
    void decrementFollowing(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("update UserEntity u set u.followersCount = u.followersCount + 1 where u.id = :id")
    void incrementFollowers(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("update UserEntity u set u.followersCount = CASE WHEN u.followersCount > 0 THEN u.followersCount - 1 ELSE 0 END where u.id = :id")
    void decrementFollowers(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("update UserEntity u set u.postsCount = u.postsCount + 1 where u.id = :id")
    void incrementPosts(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("update UserEntity u set u.postsCount = CASE WHEN u.postsCount > 0 THEN u.postsCount - 1 ELSE 0 END where u.id = :id")
    void decrementPosts(@Param("id") Long id);

    Optional<UserEntity> findByEmail(String email);
}
