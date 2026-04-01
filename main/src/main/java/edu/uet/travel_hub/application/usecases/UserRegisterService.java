package edu.uet.travel_hub.application.usecases;

import edu.uet.travel_hub.application.port.in.UserRegisterUseCase;
import edu.uet.travel_hub.application.port.out.UserRepository;

public class UserRegisterService implements UserRegisterUseCase {
    private final UserRepository userRepository;

    public UserRegisterService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
