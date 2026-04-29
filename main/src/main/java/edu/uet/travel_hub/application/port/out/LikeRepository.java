package edu.uet.travel_hub.application.port.out;

import java.util.Collection;
import java.util.Set;

import edu.uet.travel_hub.domain.model.LikeModel;

public interface LikeRepository {
    LikeModel save(Long userId, Long postId);

    void delete(Long userId, Long postId);

    boolean exists(Long userId, Long postId);

    Set<Long> findLikedPostIds(Long userId, Collection<Long> postIds);
}
