package edu.uet.travel_hub.application.port.in;

import edu.uet.travel_hub.domain.dto.request.ChangePasswordRequest;

public interface ChangePasswordUseCase {
    void changePassword(Long userId, ChangePasswordRequest request);
}
