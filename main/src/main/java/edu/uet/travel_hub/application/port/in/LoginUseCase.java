package edu.uet.travel_hub.application.port.in;

import edu.uet.travel_hub.application.dto.request.LoginRequest;
import edu.uet.travel_hub.application.dto.response.AuthResponse;

public interface LoginUseCase {
    AuthResponse login(LoginRequest request);
}
