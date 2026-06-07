package edu.uet.travel_hub.infrastructure.persistence.repository.jpa;

import edu.uet.travel_hub.infrastructure.persistence.entity.UserEntity;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
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

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByFirebaseUid(String firebaseUid);

    @EntityGraph(attributePaths = "interests")
    @Query("SELECT u FROM UserEntity u WHERE u.id = :id")
    Optional<UserEntity> findByIdWithInterests(@Param("id") Long id);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByUsernameAndIdNot(String username, Long id);

    Optional<UserEntity> findByRefreshToken(String refreshToken);

    @Query("""
            SELECT u FROM UserEntity u
            WHERE lower(coalesce(u.username, '')) LIKE lower(concat('%', :username, '%'))
            ORDER BY u.username ASC, u.id ASC
            """)
    Page<UserEntity> searchByUsername(@Param("username") String username, Pageable pageable);

    @Query(value = """
            SELECT u.id AS id,
                   u.username AS username,
                   u.name AS name,
                   u.avatar_url AS "avatarUrl",
                   u.followers_count AS "followersCount",
                   (COALESCE(p.post_count, 0) * 5
                       + COALESCE(l.like_count, 0) * 2
                       + COALESCE(c.comment_count, 0) * 3) AS score,
                   CASE WHEN f.id IS NULL THEN false ELSE true END AS following,
                   CASE WHEN u.id = :currentUserId THEN true ELSE false END AS "currentUser"
            FROM users u
            LEFT JOIN (
                SELECT user_id, COUNT(*) AS post_count
                FROM posts
                WHERE created_at >= :startAt AND created_at <= :endAt
                GROUP BY user_id
            ) p ON p.user_id = u.id
            LEFT JOIN (
                SELECT post.user_id, COUNT(*) AS like_count
                FROM likes reaction
                JOIN posts post ON post.id = reaction.post_id
                WHERE reaction.created_at >= :startAt AND reaction.created_at <= :endAt
                GROUP BY post.user_id
            ) l ON l.user_id = u.id
            LEFT JOIN (
                SELECT post.user_id, COUNT(*) AS comment_count
                FROM comments reaction
                JOIN posts post ON post.id = reaction.post_id
                WHERE reaction.created_at >= :startAt AND reaction.created_at <= :endAt
                GROUP BY post.user_id
            ) c ON c.user_id = u.id
            LEFT JOIN follows f
                ON f.follower_id = :currentUserId AND f.following_id = u.id
            WHERE (COALESCE(p.post_count, 0) * 5
                + COALESCE(l.like_count, 0) * 2
                + COALESCE(c.comment_count, 0) * 3) > 0
            ORDER BY score DESC,
                     COALESCE(c.comment_count, 0) DESC,
                     COALESCE(l.like_count, 0) DESC,
                     COALESCE(p.post_count, 0) DESC,
                     u.id ASC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM users u
            LEFT JOIN (
                SELECT user_id, COUNT(*) AS post_count
                FROM posts
                WHERE created_at >= :startAt AND created_at <= :endAt
                GROUP BY user_id
            ) p ON p.user_id = u.id
            LEFT JOIN (
                SELECT post.user_id, COUNT(*) AS like_count
                FROM likes reaction
                JOIN posts post ON post.id = reaction.post_id
                WHERE reaction.created_at >= :startAt AND reaction.created_at <= :endAt
                GROUP BY post.user_id
            ) l ON l.user_id = u.id
            LEFT JOIN (
                SELECT post.user_id, COUNT(*) AS comment_count
                FROM comments reaction
                JOIN posts post ON post.id = reaction.post_id
                WHERE reaction.created_at >= :startAt AND reaction.created_at <= :endAt
                GROUP BY post.user_id
            ) c ON c.user_id = u.id
            WHERE (COALESCE(p.post_count, 0) * 5
                + COALESCE(l.like_count, 0) * 2
                + COALESCE(c.comment_count, 0) * 3) > 0
            """,
            nativeQuery = true)
    Page<TopTravelerStatsProjection> findTopTravelers(
            @Param("currentUserId") Long currentUserId,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt,
            Pageable pageable);
}
