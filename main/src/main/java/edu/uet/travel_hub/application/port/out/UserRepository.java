package edu.uet.travel_hub.application.port.out;

import java.util.Optional;

import edu.uet.travel_hub.domain.model.UserModel;

public interface UserRepository {
    UserModel save(UserModel user);

    Optional<UserModel> findById(Long id);
}
