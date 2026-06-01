package edu.uet.travel_hub.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.uet.travel_hub.application.dto.request.RefreshTokenRequest;
import edu.uet.travel_hub.application.dto.response.AuthResponse;
import edu.uet.travel_hub.application.exception.UnauthorizedException;
import edu.uet.travel_hub.application.port.out.TokenProvider;
import edu.uet.travel_hub.application.port.out.UserRepository;
import edu.uet.travel_hub.domain.model.UserModel;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenProvider tokenProvider;

    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenService(userRepository, tokenProvider);
    }

    @Test
    void refreshRotatesStoredTokenAndReturnsNewSession() {
        UserModel user = UserModel.builder().id(6L).isOnboarded(true).build();
        when(tokenProvider.isValidRefreshToken("refresh-token")).thenReturn(true);
        when(userRepository.findByRefreshToken("refresh-token")).thenReturn(Optional.of(user));
        when(tokenProvider.generateAccessToken(user)).thenReturn("new-access-token");
        when(tokenProvider.generateRefreshToken(user)).thenReturn("new-refresh-token");

        AuthResponse response = service.refresh(new RefreshTokenRequest("refresh-token"));

        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(response.userId()).isEqualTo(6L);
        assertThat(response.isOnboarded()).isTrue();
        verify(userRepository).updateRefreshToken(6L, "new-refresh-token");
    }

    @Test
    void refreshRejectsInvalidToken() {
        when(tokenProvider.isValidRefreshToken("expired-token")).thenReturn(false);

        assertThatThrownBy(() -> service.refresh(new RefreshTokenRequest("expired-token")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void refreshRejectsTokenThatIsNotStoredForAUser() {
        when(tokenProvider.isValidRefreshToken("old-token")).thenReturn(true);
        when(userRepository.findByRefreshToken("old-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh(new RefreshTokenRequest("old-token")))
                .isInstanceOf(UnauthorizedException.class);
    }
}
