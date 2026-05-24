package edu.uet.travel_hub.infrastructure.persistence.repository.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.uet.travel_hub.infrastructure.persistence.entity.NotificationEntity;

import java.time.Instant;

public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, Long> {

    Page<NotificationEntity> findByUserId(Long userId, Pageable pageable);

    Page<NotificationEntity> findByUserIdAndIsReadFalse(Long userId, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update NotificationEntity n
            set n.isRead = true,
                n.readAt = :readAt
            where n.user.id = :userId
                and n.isRead = false
            """)
    void markAllUnreadAsReadByUserId(@Param("userId") Long userId, @Param("readAt") Instant readAt);
}
