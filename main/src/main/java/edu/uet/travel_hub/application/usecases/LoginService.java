package edu.uet.travel_hub.application.usecases;

import edu.uet.travel_hub.application.dto.request.LoginRequest;
import edu.uet.travel_hub.application.dto.response.AuthResponse;
import edu.uet.travel_hub.application.port.in.LoginUseCase;
import edu.uet.travel_hub.application.exception.UnauthorizedException;
import edu.uet.travel_hub.application.port.out.PasswordEncoder;
import edu.uet.travel_hub.application.port.out.TokenProvider;
import edu.uet.travel_hub.application.port.out.UserRepository;
import edu.uet.travel_hub.domain.model.UserModel;
import org.springframework.stereotype.Service;

@Service
public class LoginService implements LoginUseCase {
    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;

    public LoginService(UserRepository userRepository,
            TokenProvider tokenProvider,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email();
        String password = request.password();

        UserModel userModel = this.userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Email or password is incorrect"));
        if (!passwordEncoder.matches(password, userModel.getHashPassword())) {
            throw new UnauthorizedException("Email or password is incorrect");
        }
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
