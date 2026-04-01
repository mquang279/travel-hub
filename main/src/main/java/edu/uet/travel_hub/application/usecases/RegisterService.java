package edu.uet.travel_hub.application.usecases;

import edu.uet.travel_hub.application.port.in.RegisterUseCase;
import edu.uet.travel_hub.application.port.out.UserRepository;

public class RegisterService implements RegisterUseCase {
    private final UserRepository userRepository;

    public RegisterService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
