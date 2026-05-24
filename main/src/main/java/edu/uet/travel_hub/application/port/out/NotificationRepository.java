package edu.uet.travel_hub.application.port.out;

import edu.uet.travel_hub.application.dto.response.PaginationResponse;
import edu.uet.travel_hub.domain.enums.NotificationType;
import edu.uet.travel_hub.domain.model.NotificationModel;

public interface NotificationRepository {
    void save(Long userId, String title, String body, NotificationType type, Long targetId);

    PaginationResponse<NotificationModel> get(Long userId, int page, int pageSize);

    PaginationResponse<NotificationModel> getUnread(Long userId, int page, int pageSize);

    void markAllUnreadAsRead(Long userId);
}
