package edu.uet.travel_hub.infrastructure.persistence.repository.impl;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import edu.uet.travel_hub.application.dto.response.PaginationResponse;
import edu.uet.travel_hub.application.port.out.NotificationRepository;
import edu.uet.travel_hub.domain.enums.NotificationType;
import edu.uet.travel_hub.domain.model.NotificationModel;
import edu.uet.travel_hub.infrastructure.persistence.entity.NotificationEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.UserEntity;
import edu.uet.travel_hub.infrastructure.persistence.mapper.NotificationPersistenceMapper;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.NotificationJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.UserJpaRepository;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@Repository
public class NotificationRepositoryImpl implements NotificationRepository {
    private final NotificationJpaRepository notificationJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final NotificationPersistenceMapper mapper;

    @Override
    public void save(Long userId, String title, String body, NotificationType type, Long targetId) {
        UserEntity userEntity = this.userJpaRepository.findById(userId).get();
        NotificationEntity notificationEntity = NotificationEntity.builder()
                .body(body)
                .isRead(false)
                .title(title)
                .user(userEntity)
                .type(type)
                .targetId(targetId)
                .build();
        this.notificationJpaRepository.save(notificationEntity);
    }

    @Override
    public PaginationResponse<NotificationModel> get(Long userId, int page, int pageSize) {
        PageRequest request = PageRequest.of(
                page,
                pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Page<NotificationEntity> notifications = this.notificationJpaRepository.findByUserId(userId, request);
        return toPaginationResponse(notifications);
    }

    @Override
    public PaginationResponse<NotificationModel> getUnread(Long userId, int page, int pageSize) {
        PageRequest request = PageRequest.of(
                page,
                pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Page<NotificationEntity> notifications = this.notificationJpaRepository.findByUserIdAndIsReadFalse(userId,
                request);
        return toPaginationResponse(notifications);
    }

    @Override
    @Transactional
    public void markAllUnreadAsRead(Long userId) {
        this.notificationJpaRepository.markAllUnreadAsReadByUserId(userId, Instant.now());
    }

    private PaginationResponse<NotificationModel> toPaginationResponse(Page<NotificationEntity> notifications) {
        return new PaginationResponse<NotificationModel>(
                notifications.getNumber(),
                notifications.getSize(),
                notifications.getTotalPages(),
                notifications.getTotalElements(),
                notifications.getContent().stream().map(mapper::toModel).toList());
    }
}
