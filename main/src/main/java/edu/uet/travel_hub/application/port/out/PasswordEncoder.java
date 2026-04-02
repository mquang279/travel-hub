package edu.uet.travel_hub.application.port.out;

public interface PasswordEncoder {
    String encode(String rawPassword);
}
