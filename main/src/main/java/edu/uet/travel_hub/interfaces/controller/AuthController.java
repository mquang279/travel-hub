package edu.uet.travel_hub.interfaces.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import edu.uet.travel_hub.application.dto.request.LoginRequest;
import edu.uet.travel_hub.application.dto.request.FirebaseSessionRequest;
import edu.uet.travel_hub.application.dto.request.RefreshTokenRequest;
import edu.uet.travel_hub.application.dto.request.RegisterRequest;
import edu.uet.travel_hub.application.dto.response.AuthResponse;
import edu.uet.travel_hub.application.exception.UnauthorizedException;
import edu.uet.travel_hub.application.port.in.LoginUseCase;
import edu.uet.travel_hub.application.port.in.RegisterUseCase;
import edu.uet.travel_hub.application.usecases.FirebaseUserSyncService;
import edu.uet.travel_hub.application.usecases.RefreshTokenService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final String BEARER_PREFIX = "Bearer ";
    private final FirebaseAuth firebaseAuth;
    private final FirebaseUserSyncService firebaseUserSyncService;
    private final LoginUseCase loginUseCase;
    private final RegisterUseCase registerUseCase;
    private final RefreshTokenService refreshTokenService;

    public AuthController(
            FirebaseAuth firebaseAuth,
            FirebaseUserSyncService firebaseUserSyncService,
            LoginUseCase loginUseCase,
            RegisterUseCase registerUseCase,
            RefreshTokenService refreshTokenService) {
        this.firebaseAuth = firebaseAuth;
        this.firebaseUserSyncService = firebaseUserSyncService;
        this.loginUseCase = loginUseCase;
        this.registerUseCase = registerUseCase;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(this.loginUseCase.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(this.registerUseCase.register(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(this.refreshTokenService.refresh(request));
    }

    @PostMapping("/session")
    public ResponseEntity<AuthResponse> syncSession(
            @RequestHeader("Authorization") String authorization,
            @RequestBody(required = false) FirebaseSessionRequest request) {
        try {
            FirebaseToken token = firebaseAuth.verifyIdToken(extractBearerToken(authorization));
            return ResponseEntity.ok(this.firebaseUserSyncService.syncSession(token, request));
        } catch (FirebaseAuthException exception) {
            throw new UnauthorizedException("Firebase ID token is invalid or expired");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new IllegalArgumentException("Missing Firebase ID token");
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new IllegalArgumentException("Missing Firebase ID token");
        }
        return token;
    }
}
