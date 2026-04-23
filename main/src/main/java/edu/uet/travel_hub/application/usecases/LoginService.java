package edu.uet.travel_hub.application.usecases;

import org.springframework.stereotype.Service;

import edu.uet.travel_hub.application.dto.request.LoginRequest;
import edu.uet.travel_hub.application.dto.response.AuthResponse;
import edu.uet.travel_hub.application.port.in.AuthenticationPort;
import edu.uet.travel_hub.application.port.in.LoginUseCase;
import edu.uet.travel_hub.application.port.out.TokenProvider;
import edu.uet.travel_hub.application.port.out.UserRepository;
import edu.uet.travel_hub.domain.model.UserModel;

@Service
public class LoginService implements LoginUseCase {
    private final AuthenticationPort authenticationPort;
    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;

    public LoginService(UserRepository userRepository,
            TokenProvider tokenProvider, AuthenticationPort authenticationPort) {
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
        this.authenticationPort = authenticationPort;
    }

    public AuthResponse login(LoginRequest request) {
        this.authenticationPort.authentication(request.email(), request.password());

        String email = request.email();

        UserModel userModel = this.userRepository.findByEmail(email).get();
        String accessToken = this.tokenProvider.generateAccessToken(userModel);

        String refreshToken = this.tokenProvider.generateRefreshToken(userModel);
        this.userRepository.updateRefreshToken(userModel.getId(), refreshToken);

        AuthResponse response = new AuthResponse(
                accessToken,
                refreshToken,
                userModel.getId(),
                userModel.isOnboarded());
        return response;
    }
}
