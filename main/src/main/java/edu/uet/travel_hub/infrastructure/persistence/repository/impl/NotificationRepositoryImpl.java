package edu.uet.travel_hub.infrastructure.persistence.repository.impl;

import org.springframework.stereotype.Repository;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

import edu.uet.travel_hub.application.port.out.NotificationRepository;
import edu.uet.travel_hub.infrastructure.persistence.entity.NotificationEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.UserEntity;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.NotificationJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.UserJpaRepository;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@Repository
public class NotificationRepositoryImpl implements NotificationRepository {
    private final NotificationJpaRepository notificationJpaRepository;
    private final UserJpaRepository userJpaRepository;

    @Override
    public void save(Long userId, String title, String body) {
        UserEntity userEntity = this.userJpaRepository.findById(userId).get();
        NotificationEntity notificationEntity = NotificationEntity.builder()
                .body(body)
                .isRead(false)
                .title(title)
                .user(userEntity)
                .build();
        this.notificationJpaRepository.save(notificationEntity);
    }

}
