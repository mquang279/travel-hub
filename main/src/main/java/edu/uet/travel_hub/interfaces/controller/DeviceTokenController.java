package edu.uet.travel_hub.interfaces.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.uet.travel_hub.application.dto.request.AddDeviceTokenRequest;
import edu.uet.travel_hub.application.port.in.AddDeviceTokenUseCase;
import lombok.AllArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/devices")
@AllArgsConstructor
public class DeviceTokenController {
    private final AddDeviceTokenUseCase addDeviceTokenUseCase;

    @PostMapping("/token")
    public ResponseEntity<Void> addDeviceToken(@RequestBody AddDeviceTokenRequest request) {
        this.addDeviceTokenUseCase.addDevice(request);
        return ResponseEntity.noContent().build();
    }
}
