package edu.uet.travel_hub.application.port.in;

import edu.uet.travel_hub.domain.dto.response.UserProfileResponse;

public interface GetUserProfileUseCase {
    UserProfileResponse getProfile(Long currentUserId, Long targetUserId);
}
