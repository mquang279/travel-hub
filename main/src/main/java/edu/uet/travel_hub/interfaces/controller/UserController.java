package edu.uet.travel_hub.interfaces.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.uet.travel_hub.application.dto.request.RegisterRequest;
import edu.uet.travel_hub.application.dto.response.AuthResponse;
import edu.uet.travel_hub.application.usecases.RegisterService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/auth")
public class UserController {
    private final RegisterService registerService;

    public UserController(RegisterService registerService) {
        this.registerService = registerService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        AuthResponse response = this.registerService.register(request);
        return ResponseEntity.ok().body(response);
    }

}
