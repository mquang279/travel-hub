package edu.uet.travel_hub.application.port.out;

public interface PushNotificationSender {
    void send(Long userId, String title, String body);
}
