package edu.uet.travel_hub.application.port.in;

public interface SaveNotificationUseCase {
    void save(Long userId, String title, String body);
}
