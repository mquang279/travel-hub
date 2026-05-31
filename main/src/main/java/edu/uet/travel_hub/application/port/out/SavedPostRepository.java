package edu.uet.travel_hub.application.port.out;

import java.util.Collection;
import java.util.Set;

public interface SavedPostRepository {
    void save(Long userId, Long postId);

    void delete(Long userId, Long postId);

    boolean exists(Long userId, Long postId);

    Set<Long> findSavedPostIds(Long userId, Collection<Long> postIds);
}
