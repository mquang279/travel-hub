package edu.uet.travel_hub.infrastructure.security;

import java.util.Map;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
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

        Optional<Long> jwtUserId = extractJwtUserId(authentication);
        if (jwtUserId.isPresent()) {
            return jwtUserId;
        }

        String email = authentication.getName();
        return userJpaRepository.findByEmail(email)
                .map(UserEntity::getId);
    }

    private Optional<Long> extractJwtUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Jwt jwt)) {
            return Optional.empty();
        }

        Object userClaim = jwt.getClaim("user");
        if (!(userClaim instanceof Map<?, ?> user)) {
            return Optional.empty();
        }

        Object id = user.get("id");
        if (id instanceof Number number) {
            return Optional.of(number.longValue());
        }
        if (id instanceof String value && !value.isBlank()) {
            try {
                return Optional.of(Long.parseLong(value));
            } catch (NumberFormatException ignored) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}
