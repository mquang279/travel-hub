package edu.uet.travel_hub.interfaces.controller;

import edu.uet.travel_hub.application.port.in.FollowUserUseCase;
import edu.uet.travel_hub.application.port.in.GetFollowersUseCase;
import edu.uet.travel_hub.application.port.in.GetFollowingUseCase;
import edu.uet.travel_hub.application.port.in.UnfollowUserUseCase;
import edu.uet.travel_hub.application.port.out.CurrentUserProvider;
import edu.uet.travel_hub.domain.dto.response.UserFollowResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class FollowController {

    private final GetFollowersUseCase getFollowersUseCase;
    private final GetFollowingUseCase getFollowingUseCase;
    private final FollowUserUseCase followUserUseCase;
    private final UnfollowUserUseCase unfollowUserUseCase;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/{userId}/followers")
    public ResponseEntity<Page<UserFollowResponse>> getFollowers(
            @PathVariable Long userId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "30") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Long currentUserId = currentUserProvider.getOptionalCurrentUserId().orElse(null);
        return ResponseEntity.ok(getFollowersUseCase.getFollowers(currentUserId, userId, pageable));
    }

    @GetMapping("/{userId}/following")
    public ResponseEntity<Page<UserFollowResponse>> getFollowing(
            @PathVariable Long userId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "30") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Long currentUserId = currentUserProvider.getOptionalCurrentUserId().orElse(null);
        return ResponseEntity.ok(getFollowingUseCase.getFollowing(currentUserId, userId, pageable));
    }

    @PostMapping("/{targetUserId}/follow")
    public ResponseEntity<Void> followUser(@PathVariable Long targetUserId) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        followUserUseCase.followUser(currentUserId, targetUserId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{targetUserId}/follow")
    public ResponseEntity<Void> unfollowUser(@PathVariable Long targetUserId) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        unfollowUserUseCase.unfollowUser(currentUserId, targetUserId);
        return ResponseEntity.ok().build();
    }
}
