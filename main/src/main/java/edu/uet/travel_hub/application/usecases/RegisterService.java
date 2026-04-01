package edu.uet.travel_hub.application.usecases;

import org.springframework.security.core.userdetails.User;

import edu.uet.travel_hub.application.dto.request.RegisterRequest;
import edu.uet.travel_hub.application.dto.response.AuthResponse;
import edu.uet.travel_hub.application.port.in.RegisterUseCase;
import edu.uet.travel_hub.application.port.out.PasswordEncoder;
import edu.uet.travel_hub.application.port.out.TokenProvider;
import edu.uet.travel_hub.application.port.out.UserRepository;
import edu.uet.travel_hub.domain.model.UserModel;

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
    public AuthResponse register(RegisterRequest request) {
        String hashPassword = encoder.encode(request.password());
        UserModel user = new UserModel(request.email(), request.username(), hashPassword);
        this.userRepository.save(user);
        String accessToken = this.tokenProvider.generateAccessToken(user);
        String refreshToken = this.tokenProvider.generateRefreshToken(user);
        AuthResponse response = new AuthResponse(accessToken, refreshToken, user.getId());
        return response;
    }
}
