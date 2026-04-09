package edu.uet.travel_hub.interfaces.controller;

import edu.uet.travel_hub.application.port.in.FollowUseCase;
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

    private final FollowUseCase followUseCase;

    private static final Long MOCK_CURRENT_USER_ID = 1L;

    @GetMapping("/{userId}/followers")
    public ResponseEntity<Page<UserFollowResponse>> getFollowers(
            @PathVariable Long userId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "30") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(followUseCase.getFollowers(MOCK_CURRENT_USER_ID, userId, pageable));
    }

    @GetMapping("/{userId}/following")
    public ResponseEntity<Page<UserFollowResponse>> getFollowing(
            @PathVariable Long userId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "30") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(followUseCase.getFollowing(MOCK_CURRENT_USER_ID, userId, pageable));
    }

    @PostMapping("/{targetUserId}/follow")
    public ResponseEntity<Void> followUser(@PathVariable Long targetUserId) {
        followUseCase.followUser(MOCK_CURRENT_USER_ID, targetUserId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{targetUserId}/follow")
    public ResponseEntity<Void> unfollowUser(@PathVariable Long targetUserId) {
        followUseCase.unfollowUser(MOCK_CURRENT_USER_ID, targetUserId);
        return ResponseEntity.ok().build();
    }
}
