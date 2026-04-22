package edu.uet.travel_hub.application.usecases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.uet.travel_hub.application.dto.request.RegisterRequest;
import edu.uet.travel_hub.application.dto.response.AuthResponse;
import edu.uet.travel_hub.application.port.in.RegisterUseCase;
import edu.uet.travel_hub.application.port.out.PasswordEncoder;
import edu.uet.travel_hub.application.port.out.TokenProvider;
import edu.uet.travel_hub.application.port.out.UserRepository;
import edu.uet.travel_hub.domain.enums.Role;
import edu.uet.travel_hub.domain.model.UserModel;

@Service
public class RegisterService implements RegisterUseCase {
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final TokenProvider tokenProvider;

    public RegisterService(UserRepository userRepository, PasswordEncoder encoder, TokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.tokenProvider = tokenProvider;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String hashPassword = encoder.encode(request.password());
        UserModel user = this.userRepository.register(request.email(), request.username(), hashPassword, Role.USER);
        String accessToken = this.tokenProvider.generateAccessToken(user);
        String refreshToken = this.tokenProvider.generateRefreshToken(user);
        AuthResponse response = new AuthResponse(accessToken, refreshToken, user.getId());
        return response;
    }
}
