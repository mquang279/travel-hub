package edu.uet.travel_hub.infrastructure.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import edu.uet.travel_hub.application.port.out.CurrentUserProvider;
import edu.uet.travel_hub.infrastructure.persistence.entity.UserJpaEntity;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.UserJpaRepository;

@Component
public class SecurityCurrentUserProvider implements CurrentUserProvider {
    private final UserJpaRepository userJpaRepository;

    public SecurityCurrentUserProvider(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // TODO: Xử lý Exception
        String email = authentication.getName();
        UserJpaEntity user = userJpaRepository.findByEmail(email).get();
        return user.getId();
    }

}
