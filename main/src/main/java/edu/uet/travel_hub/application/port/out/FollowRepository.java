package edu.uet.travel_hub.application.port.out;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import edu.uet.travel_hub.domain.model.FollowModel;
import edu.uet.travel_hub.domain.model.UserModel;

public interface FollowRepository {
    Page<UserModel> findFollowers(Long targetUserId, Pageable pageable);

    Page<UserModel> findFollowing(Long targetUserId, Pageable pageable);

    Optional<FollowModel> findByFollowerIdAndFollowingId(Long followerId, Long followingId);

    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    FollowModel save(FollowModel followModel);

    void deleteByFollowerIdAndFollowingId(Long followerId, Long followingId);
}
