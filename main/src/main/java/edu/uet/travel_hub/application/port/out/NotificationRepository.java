package edu.uet.travel_hub.application.port.out;

public interface NotificationRepository {
    void save(Long userId, String title, String body);
}
