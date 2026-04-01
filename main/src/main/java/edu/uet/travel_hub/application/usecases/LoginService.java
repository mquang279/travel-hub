package edu.uet.travel_hub.application.usecases;

import edu.uet.travel_hub.application.dto.request.LoginRequest;
import edu.uet.travel_hub.application.dto.response.AuthResponse;
import edu.uet.travel_hub.application.port.in.LoginUseCase;
import edu.uet.travel_hub.application.port.out.UserRepository;

public class LoginService implements LoginUseCase {
    private final UserRepository userRepository;

    public LoginService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public AuthResponse login(LoginRequest request) {
        
    }
}
