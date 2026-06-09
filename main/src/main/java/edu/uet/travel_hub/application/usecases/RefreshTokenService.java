package edu.uet.travel_hub.application.usecases;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import edu.uet.travel_hub.application.dto.request.RefreshTokenRequest;
import edu.uet.travel_hub.application.dto.response.AuthResponse;
import edu.uet.travel_hub.application.exception.UnauthorizedException;
import edu.uet.travel_hub.application.port.out.TokenProvider;
import edu.uet.travel_hub.application.port.out.UserRepository;
import edu.uet.travel_hub.domain.model.UserModel;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenService {
    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;

    public RefreshTokenService(UserRepository userRepository, TokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request == null ? null : request.refreshToken();
        if (!this.tokenProvider.isValidRefreshToken(refreshToken)) {
            throw new UnauthorizedException("Refresh token is invalid or expired");
        }

        UserModel user = this.userRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new UnauthorizedException("Refresh token is no longer active"));
        String nextAccessToken = this.tokenProvider.generateAccessToken(user);
        String nextRefreshToken = this.tokenProvider.generateRefreshToken(user);
        this.userRepository.updateRefreshToken(user.getId(), nextRefreshToken);

        return new AuthResponse(
                nextAccessToken,
                nextRefreshToken,
                user.getId(),
                user.isOnboarded());
    }
}
