package edu.uet.travel_hub.infrastructure.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import edu.uet.travel_hub.infrastructure.persistence.entity.UserJpaEntity;
import edu.uet.travel_hub.infrastructure.persistence.repository.UserJpaRepository;

@Component("userDetailsService")
public class UserDetailCustom implements UserDetailsService {
    private final UserJpaRepository userJpaRepository;

    public UserDetailCustom(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserJpaEntity user = userJpaRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User with email " + email + " not found."));

        return User.builder()
                .username(user.getEmail())
                .password(user.getHashPassword())
                .authorities("USER")
                .build();
    }
}
