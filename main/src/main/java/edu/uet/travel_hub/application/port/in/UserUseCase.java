package edu.uet.travel_hub.application.port.in;

import edu.uet.travel_hub.domain.dto.request.UpdateProfileRequest;
import edu.uet.travel_hub.domain.dto.response.UserProfileResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UserUseCase {
    UserProfileResponse getProfile(Long currentUserId, Long targetUserId);
    UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request);
    String uploadAvatar(Long userId, MultipartFile file);
}