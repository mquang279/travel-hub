package edu.uet.travel_hub.interfaces.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.uet.travel_hub.application.dto.request.LoginRequest;
import edu.uet.travel_hub.application.dto.request.RegisterRequest;
import edu.uet.travel_hub.application.dto.response.AuthResponse;
import edu.uet.travel_hub.application.usecases.LoginService;
import edu.uet.travel_hub.application.usecases.LogoutService;
import edu.uet.travel_hub.application.usecases.RegisterService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final RegisterService registerService;
    private final LoginService loginService;
    private final LogoutService logoutService;

    public AuthController(RegisterService registerService, LoginService loginService, LogoutService logoutService) {
        this.registerService = registerService;
        this.loginService = loginService;
        this.logoutService = logoutService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        AuthResponse response = this.registerService.register(request);
        return ResponseEntity.ok().body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loging(@RequestBody LoginRequest request) {
        AuthResponse response = this.loginService.login(request);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        System.out.println(authentication);
        return ResponseEntity.ok().body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        this.logoutService.logout(email);
        return ResponseEntity.noContent().build();
    }

}
