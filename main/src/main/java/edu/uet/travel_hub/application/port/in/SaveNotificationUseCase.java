package edu.uet.travel_hub.application.port.in;

import edu.uet.travel_hub.domain.enums.NotificationType;

public interface SaveNotificationUseCase {
    void save(Long userId, String title, String body, NotificationType type, Long targetId);
}
