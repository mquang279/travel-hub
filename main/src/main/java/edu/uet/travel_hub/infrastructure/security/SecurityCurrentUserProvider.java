package edu.uet.travel_hub.infrastructure.security;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import edu.uet.travel_hub.application.exception.UnauthorizedException;
import edu.uet.travel_hub.application.port.out.CurrentUserProvider;
import edu.uet.travel_hub.infrastructure.persistence.entity.UserEntity;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.UserJpaRepository;

@Component
public class SecurityCurrentUserProvider implements CurrentUserProvider {
    private final UserJpaRepository userJpaRepository;

    public SecurityCurrentUserProvider(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public Long getCurrentUserId() {
        return getOptionalCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Current user is not authenticated"));
    }

    @Override
    public Optional<Long> getOptionalCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return Optional.empty();
        }

        String email = authentication.getName();
        return userJpaRepository.findByEmail(email)
                .map(UserEntity::getId);
    }

}
