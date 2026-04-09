package edu.uet.travel_hub.application.port.in;

import edu.uet.travel_hub.domain.dto.request.UpdateProfileRequest;
import edu.uet.travel_hub.domain.dto.response.UserProfileResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UserUseCase {
    // TODO: Tách ra thành 3 usecase: GetUserProfileUseCase, UpdateProfileUseCase
    // Cái upload thì để sau cũng được, tại nhiều chỗ cần sau này làm 1 cái tập trung
    UserProfileResponse getProfile(Long currentUserId, Long targetUserId);
    UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request);
    String uploadAvatar(Long userId, MultipartFile file);
}