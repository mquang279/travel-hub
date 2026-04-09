package edu.uet.travel_hub.interfaces.controller;

import edu.uet.travel_hub.application.port.in.UserUseCase;
import edu.uet.travel_hub.domain.dto.request.UpdateProfileRequest;
import edu.uet.travel_hub.domain.dto.response.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserUseCase userUseCase;

	// MOCK_CURRENT_USER_ID is just for bypassing auth context temporarily
	private static final Long MOCK_CURRENT_USER_ID = 1L;

	@GetMapping("/me")
	public ResponseEntity<UserProfileResponse> getMyProfile() {
		return ResponseEntity.ok(userUseCase.getProfile(MOCK_CURRENT_USER_ID, MOCK_CURRENT_USER_ID));
	}

	@GetMapping("/{userId}")
	public ResponseEntity<UserProfileResponse> getProfile(@PathVariable Long userId) {
		return ResponseEntity.ok(userUseCase.getProfile(MOCK_CURRENT_USER_ID, userId));
	}

	@PutMapping("/me")
	public ResponseEntity<UserProfileResponse> updateMyProfile(@RequestBody UpdateProfileRequest request) {
		return ResponseEntity.ok(userUseCase.updateProfile(MOCK_CURRENT_USER_ID, request));
	}

	@PutMapping("/{userId}")
	public ResponseEntity<UserProfileResponse> updateUserProfile(
			@PathVariable Long userId,
			@RequestBody UpdateProfileRequest request) {
		return ResponseEntity.ok(userUseCase.updateProfile(userId, request));
	}

	@PostMapping("/me/avatar")
	public ResponseEntity<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
		String avatarUrl = userUseCase.uploadAvatar(MOCK_CURRENT_USER_ID, file);
		return ResponseEntity.ok(avatarUrl);
	}
}
