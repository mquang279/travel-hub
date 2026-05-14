package edu.uet.travel_hub.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;

import edu.uet.travel_hub.domain.model.NotificationModel;
import edu.uet.travel_hub.infrastructure.persistence.entity.NotificationEntity;

@Component
public class NotificationPersistenceMapper {
    public NotificationModel toModel(NotificationEntity entity) {
        return NotificationModel.builder()
                .title(entity.getTitle())
                .body(entity.getBody())
                .isRead(entity.getIsRead())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public NotificationEntity toEntity(NotificationModel model) {
        return NotificationEntity.builder()
                .title(model.title())
                .body(model.body())
                .isRead(model.isRead()).build();
    }
}
