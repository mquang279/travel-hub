package edu.uet.travel_hub.application.usecases;

import edu.uet.travel_hub.application.port.in.UserLoginUseCase;
import edu.uet.travel_hub.application.port.out.UserRepository;

public class UserLoginService implements UserLoginUseCase {
    private final UserRepository userRepository;

    public UserLoginService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    
}
