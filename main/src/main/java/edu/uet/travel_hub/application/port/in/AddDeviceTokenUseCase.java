package edu.uet.travel_hub.application.port.in;

import edu.uet.travel_hub.application.dto.request.AddDeviceTokenRequest;

public interface AddDeviceTokenUseCase {
    void addDevice(AddDeviceTokenRequest request);
}
