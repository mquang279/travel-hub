package edu.uet.travel_hub.application.usecases;

import org.springframework.stereotype.Service;

import edu.uet.travel_hub.application.port.in.MarkAllNotificationsAsReadUseCase;
import edu.uet.travel_hub.application.port.out.CurrentUserProvider;
import edu.uet.travel_hub.application.port.out.NotificationRepository;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class MarkAllNotificationsAsReadService implements MarkAllNotificationsAsReadUseCase {
    private final NotificationRepository notificationRepository;
    private final CurrentUserProvider currentUserProvider;

    @Override
    public void markAllAsRead() {
        Long userId = this.currentUserProvider.getCurrentUserId();
        this.notificationRepository.markAllUnreadAsRead(userId);
    }
}
