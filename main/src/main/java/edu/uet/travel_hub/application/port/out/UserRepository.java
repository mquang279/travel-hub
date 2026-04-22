package edu.uet.travel_hub.application.port.out;

import java.util.Optional;

import edu.uet.travel_hub.domain.enums.Role;
import edu.uet.travel_hub.domain.model.UserModel;

public interface UserRepository {
    UserModel save(UserModel user);

    UserModel register(String email, String username, String password, Role role);

    Optional<UserModel> findById(Long id);

    Optional<UserModel> findByEmail(String email);

    void updateRefreshToken(Long id, String refreshToken);

    void incrementFollowing(Long id);

    void decrementFollowing(Long id);

    void incrementFollowers(Long id);

    void decrementFollowers(Long id);
}
