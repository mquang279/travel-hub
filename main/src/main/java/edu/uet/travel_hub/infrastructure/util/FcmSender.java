package edu.uet.travel_hub.infrastructure.util;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

import edu.uet.travel_hub.application.port.out.PushNotificationSender;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.DeviceTokenJpaRepository;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@Component
public class FcmSender implements PushNotificationSender {
    private final DeviceTokenJpaRepository deviceTokenJpaRepository;

    @Override
    @Async("taskExecutor")
    public void send(Long userId, String title, String body) {
        var deviceTokens = deviceTokenJpaRepository.findByUserId(userId);

        deviceTokens.forEach(deviceToken -> {
            Message message = Message.builder()
                    .setToken(deviceToken.getToken())
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();

            try {
                String response = FirebaseMessaging.getInstance().send(message);
                System.out.println("Message sent successfully to device " + deviceToken.getToken() + ": " + response);
            } catch (Exception e) {
                System.out.println("Message failed to send to device " + deviceToken.getToken());
                e.printStackTrace();
            }
        });
    }

}
