package edu.uet.travel_hub.application.port.out;

import java.util.Optional;

public interface CurrentUserProvider {
    Long getCurrentUserId();

    Optional<Long> getOptionalCurrentUserId();
}
