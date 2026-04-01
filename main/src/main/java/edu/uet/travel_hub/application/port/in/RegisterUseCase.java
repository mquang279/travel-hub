package edu.uet.travel_hub.application.port.in;

import edu.uet.travel_hub.application.dto.request.RegisterRequest;
import edu.uet.travel_hub.application.dto.response.AuthResponse;

public interface RegisterUseCase {
    AuthResponse register(RegisterRequest request);
}
