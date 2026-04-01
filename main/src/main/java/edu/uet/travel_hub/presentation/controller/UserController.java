package edu.uet.travel_hub.presentation.controller;

import edu.uet.travel_hub.application.usecases.UserUseCase;
import edu.uet.travel_hub.domain.dto.request.UpdateProfileRequest;
import edu.uet.travel_hub.domain.dto.response.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserUseCase userUseCase;

    // MOCK_CURRENT_USER_ID is just for bypassing auth context temporarily
    private final Long MOCK_CURRENT_USER_ID = 1L;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile() {
        return ResponseEntity.ok(userUseCase.getProfile(MOCK_CURRENT_USER_ID));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> getProfile(@PathVariable Long userId) {
        return ResponseEntity.ok(userUseCase.getProfile(userId));
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateMyProfile(@RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userUseCase.updateProfile(MOCK_CURRENT_USER_ID, request));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> updateUserProfile(@PathVariable Long userId, @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userUseCase.updateProfile(userId, request));
    }

    @PostMapping("/me/avatar")
    public ResponseEntity<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        String avatarUrl = userUseCase.uploadAvatar(MOCK_CURRENT_USER_ID, file);
        return ResponseEntity.ok(avatarUrl);
    }
}
