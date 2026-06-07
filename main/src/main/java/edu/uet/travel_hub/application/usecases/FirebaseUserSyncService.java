package edu.uet.travel_hub.application.usecases;

import com.google.firebase.auth.FirebaseToken;
import edu.uet.travel_hub.application.dto.request.FirebaseSessionRequest;
import edu.uet.travel_hub.application.dto.response.AuthResponse;
import edu.uet.travel_hub.domain.enums.Role;
import edu.uet.travel_hub.domain.exception.UsernameAlreadyExistsException;
import edu.uet.travel_hub.infrastructure.persistence.entity.UserEntity;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.UserJpaRepository;
import edu.uet.travel_hub.infrastructure.security.FirebaseAuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class FirebaseUserSyncService {
    private final UserJpaRepository userJpaRepository;

    public FirebaseUserSyncService(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Transactional
    public FirebaseAuthenticatedUser sync(FirebaseToken token) {
        return toAuthenticatedUser(syncEntity(token, null));
    }

    @Transactional
    public AuthResponse syncSession(FirebaseToken token, FirebaseSessionRequest request) {
        UserEntity user = syncEntity(token, request);
        return new AuthResponse("", "", user.getId(), user.isOnboarded());
    }

    private UserEntity syncEntity(FirebaseToken token, FirebaseSessionRequest request) {
        String firebaseUid = token.getUid();
        String email = normalizeEmail(token.getEmail());
        String desiredUsername = normalize(request == null ? null : request.username());
        String desiredName = normalize(request == null ? null : request.name());

        UserEntity user = userJpaRepository.findByFirebaseUid(firebaseUid)
                .or(() -> email == null ? java.util.Optional.empty() : userJpaRepository.findByEmail(email))
                .orElseGet(UserEntity::new);

        boolean newUser = user.getId() == null;
        user.setFirebaseUid(firebaseUid);
        if (email != null) {
            user.setEmail(email);
        }
        if (user.getRole() == null) {
            user.setRole(Role.USER);
        }
        if (desiredName != null) {
            user.setName(desiredName);
        } else if (isBlank(user.getName())) {
            user.setName(firstNonBlank(token.getName(), email, firebaseUid));
        }

        if (desiredUsername != null) {
            ensureUsernameAvailable(desiredUsername, user.getId());
            user.setUsername(desiredUsername);
        } else if (isBlank(user.getUsername())) {
            user.setUsername(generateUsername(email, firebaseUid));
        }

        if (newUser) {
            user.setFollowersCount(0);
            user.setFollowingCount(0);
            user.setPostsCount(0);
            user.setOnboarded(false);
        }

        return userJpaRepository.save(user);
    }

    private FirebaseAuthenticatedUser toAuthenticatedUser(UserEntity user) {
        return new FirebaseAuthenticatedUser(
                user.getId(),
                user.getFirebaseUid(),
                user.getEmail(),
                user.getUsername(),
                user.getRole());
    }

    private void ensureUsernameAvailable(String username, Long userId) {
        boolean taken = userId == null
                ? userJpaRepository.existsByUsername(username)
                : userJpaRepository.existsByUsernameAndIdNot(username, userId);
        if (taken) {
            throw new UsernameAlreadyExistsException(username);
        }
    }

    private String generateUsername(String email, String firebaseUid) {
        String base = "user";
        if (email != null) {
            int atIndex = email.indexOf('@');
            base = atIndex > 0 ? email.substring(0, atIndex) : email;
        }
        base = base.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        if (base.length() < 3) {
            base = "user_" + base;
        }

        String candidate = base;
        int suffix = 1;
        while (userJpaRepository.existsByUsername(candidate)) {
            candidate = base + "_" + suffix;
            suffix++;
        }
        return candidate;
    }

    private String normalizeEmail(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String firstNonBlank(String first, String second, String fallback) {
        String normalizedFirst = normalize(first);
        if (normalizedFirst != null) {
            return normalizedFirst;
        }
        String normalizedSecond = normalize(second);
        return normalizedSecond == null ? fallback : normalizedSecond;
    }
}
