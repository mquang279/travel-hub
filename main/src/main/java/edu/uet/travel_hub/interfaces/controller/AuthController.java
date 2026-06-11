package edu.uet.travel_hub.interfaces.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.uet.travel_hub.application.dto.request.LoginRequest;
import edu.uet.travel_hub.application.dto.request.RefreshTokenRequest;
import edu.uet.travel_hub.application.dto.request.RegisterRequest;
import edu.uet.travel_hub.application.dto.response.AuthResponse;
import edu.uet.travel_hub.application.port.in.LogoutUseCase;
import edu.uet.travel_hub.application.port.in.LoginUseCase;
import edu.uet.travel_hub.application.port.in.RegisterUseCase;
import edu.uet.travel_hub.application.usecases.RefreshTokenService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final RegisterUseCase registerUseCase;
    private final LoginUseCase loginUseCase;
    private final RefreshTokenService refreshTokenService;
    private final LogoutUseCase logoutUseCase;

    public AuthController(
            RegisterUseCase registerUseCase,
            LoginUseCase loginUseCase,
            RefreshTokenService refreshTokenService,
            LogoutUseCase logoutUseCase) {
        this.registerUseCase = registerUseCase;
        this.loginUseCase = loginUseCase;
        this.refreshTokenService = refreshTokenService;
        this.logoutUseCase = logoutUseCase;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(registerUseCase.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(loginUseCase.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(refreshTokenService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication) {
        logoutUseCase.logout(authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
