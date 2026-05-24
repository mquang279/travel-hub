package edu.uet.travel_hub.application.port.out;

public interface SavedPostRepository {
    void save(Long userId, Long postId);

    boolean exists(Long userId, Long postId);
}
