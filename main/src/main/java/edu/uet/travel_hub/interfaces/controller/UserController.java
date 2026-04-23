package edu.uet.travel_hub.interfaces.controller;

import edu.uet.travel_hub.application.port.in.GetUserProfileUseCase;
import edu.uet.travel_hub.application.port.in.GetPostsOfUserUseCase;
import edu.uet.travel_hub.application.port.in.UpdateProfileUseCase;
import edu.uet.travel_hub.application.port.in.UploadAvatarUseCase;
import edu.uet.travel_hub.application.dto.response.PaginationResponse;
import edu.uet.travel_hub.application.dto.response.PostResponse;
import edu.uet.travel_hub.application.mapper.PostMapper;
import edu.uet.travel_hub.application.port.out.CurrentUserProvider;
import edu.uet.travel_hub.application.usecases.UserPreferenceService;
import edu.uet.travel_hub.domain.dto.request.PreferenceUpdateRequest;
import edu.uet.travel_hub.domain.dto.request.UpdateProfileRequest;
import edu.uet.travel_hub.domain.dto.response.PreferenceResponse;
import edu.uet.travel_hub.domain.dto.response.UserProfileResponse;
import edu.uet.travel_hub.domain.model.PostModel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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

	private final GetUserProfileUseCase getUserProfileUseCase;
	private final GetPostsOfUserUseCase getPostsOfUserUseCase;
	private final UpdateProfileUseCase updateProfileUseCase;
	private final UploadAvatarUseCase uploadAvatarUseCase;
	private final UserPreferenceService userPreferenceService;
	private final CurrentUserProvider currentUserProvider;
	private final PostMapper postMapper;

	@GetMapping("/me")
	public ResponseEntity<UserProfileResponse> getMyProfile() {
		Long currentUserId = currentUserProvider.getCurrentUserId();
		return ResponseEntity.ok(getUserProfileUseCase.getProfile(currentUserId, currentUserId));
	}

	@GetMapping("/{userId}")
	public ResponseEntity<UserProfileResponse> getProfile(@PathVariable Long userId) {
		Long currentUserId = currentUserProvider.getOptionalCurrentUserId().orElse(null);
		return ResponseEntity.ok(getUserProfileUseCase.getProfile(currentUserId, userId));
	}

	@GetMapping("/{userId}/posts")
	public ResponseEntity<PaginationResponse<PostResponse>> getPostsOfUser(
			@PathVariable Long userId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "5") int pageSize) {
		PaginationResponse<PostModel> posts = getPostsOfUserUseCase.get(userId, page, pageSize);
		PaginationResponse<PostResponse> response = new PaginationResponse<>(
				posts.pageNumber(),
				posts.pageSize(),
				posts.totalPages(),
				posts.totalElements(),
				posts.data().stream().map(postMapper::toDto).toList());
		return ResponseEntity.ok(response);
	}

	@PutMapping("/me")
	public ResponseEntity<UserProfileResponse> updateMyProfile(@RequestBody UpdateProfileRequest request) {
		Long currentUserId = currentUserProvider.getCurrentUserId();
		return ResponseEntity.ok(updateProfileUseCase.updateProfile(currentUserId, request));
	}

	@PutMapping("/{userId}")
	public ResponseEntity<UserProfileResponse> updateUserProfile(
			@PathVariable Long userId,
			@RequestBody UpdateProfileRequest request) {
		return ResponseEntity.ok(updateProfileUseCase.updateProfile(userId, request));
	}

	@GetMapping("/{userId}/preferences")
	public ResponseEntity<PreferenceResponse> getPreferences(@PathVariable Long userId) {
		Long currentUserId = currentUserProvider.getCurrentUserId();
		ensureCurrentUser(currentUserId, userId);
		return ResponseEntity.ok(userPreferenceService.getPreferences(userId));
	}

	@PutMapping("/{userId}/preferences")
	public ResponseEntity<PreferenceResponse> updatePreferences(
			@PathVariable Long userId,
			@RequestBody PreferenceUpdateRequest request) {
		Long currentUserId = currentUserProvider.getCurrentUserId();
		ensureCurrentUser(currentUserId, userId);
		return ResponseEntity.ok(userPreferenceService.updatePreferences(userId, request));
	}

	@PostMapping("/me/avatar")
	public ResponseEntity<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
		Long currentUserId = currentUserProvider.getCurrentUserId();
		String avatarUrl = uploadAvatarUseCase.uploadAvatar(currentUserId, file);
		return ResponseEntity.ok(avatarUrl);
	}

	private void ensureCurrentUser(Long currentUserId, Long userId) {
		if (!currentUserId.equals(userId)) {
			throw new AccessDeniedException("Cannot access preferences of another user");
		}
	}
}
