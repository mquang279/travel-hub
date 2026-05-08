package edu.uet.travel_hub.application.usecases;

import org.springframework.stereotype.Service;

import edu.uet.travel_hub.application.port.in.SaveNotificationUseCase;
import edu.uet.travel_hub.application.port.out.NotificationRepository;
import edu.uet.travel_hub.application.port.out.PushNotificationSender;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@Service
public class SaveNotificationService implements SaveNotificationUseCase {
    private final NotificationRepository notificationRepository;
    private final PushNotificationSender sender;

    @Override
    public void save(Long userId, String title, String body) {
        this.sender.send(userId, title, body);
        this.notificationRepository.save(userId, title, body);
    }
}
