package edu.uet.travel_hub.application.usecases;

import org.springframework.stereotype.Service;

import edu.uet.travel_hub.application.port.in.LogoutUseCase;
import edu.uet.travel_hub.application.port.out.UserRepository;
import edu.uet.travel_hub.domain.model.UserModel;

@Service
public class LogoutService implements LogoutUseCase {
    private final UserRepository userRepository;

    public LogoutService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void logout(String email) {
        UserModel user = this.userRepository.findByEmail(email).get();
        this.userRepository.updateRefreshToken(user.getId(), null);
    }
}
