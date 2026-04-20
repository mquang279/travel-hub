package edu.uet.travel_hub.application.port.in;

import edu.uet.travel_hub.domain.dto.request.UpdateProfileRequest;
import edu.uet.travel_hub.domain.dto.response.UserProfileResponse;

public interface UpdateProfileUseCase {
    UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request);
}
