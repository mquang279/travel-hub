package edu.uet.travel_hub.application.port.out;

public interface DeviceTokenRepository {
    void add(String token, Long userId);
}
