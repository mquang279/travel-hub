package edu.uet.travel_hub.application.usecases;

import edu.uet.travel_hub.application.dto.request.AddDeviceTokenRequest;
import edu.uet.travel_hub.application.port.in.AddDeviceTokenUseCase;
import edu.uet.travel_hub.application.port.out.CurrentUserProvider;
import edu.uet.travel_hub.application.port.out.DeviceTokenRepository;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class AddDeviceTokenService implements AddDeviceTokenUseCase {
    private final DeviceTokenRepository deviceTokenRepository;
    private final CurrentUserProvider currentUserProvider;

    @Override
    public void addDevice(AddDeviceTokenRequest request) {
        Long userId = this.currentUserProvider.getCurrentUserId();
        this.deviceTokenRepository.add(request.token(), userId);
    }
}
