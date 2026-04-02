package edu.uet.travel_hub.interfaces.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.uet.travel_hub.application.dto.request.LoginRequest;
import edu.uet.travel_hub.application.dto.request.RegisterRequest;
import edu.uet.travel_hub.application.dto.response.AuthResponse;
import edu.uet.travel_hub.application.usecases.LoginService;
import edu.uet.travel_hub.application.usecases.RegisterService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/auth")
public class UserController {
    private final RegisterService registerService;
    private final LoginService loginService;

    public UserController(RegisterService registerService, LoginService loginService) {
        this.registerService = registerService;
        this.loginService = loginService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        AuthResponse response = this.registerService.register(request);
        return ResponseEntity.ok().body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loging(@RequestBody LoginRequest request) {
        AuthResponse response = this.loginService.login(request);        
        return ResponseEntity.ok().body(response);
    }
    
}
