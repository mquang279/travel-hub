package edu.uet.travel_hub.application.usecases;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.uet.travel_hub.application.exception.ResourceNotFoundException;
import edu.uet.travel_hub.application.port.out.UserRepository;
import edu.uet.travel_hub.domain.dto.request.PreferenceUpdateRequest;
import edu.uet.travel_hub.domain.dto.response.PreferenceResponse;
import edu.uet.travel_hub.domain.model.UserModel;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserPreferenceService {
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PreferenceResponse getPreferences(Long userId) {
        UserModel user = this.userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toResponse(user);
    }

    @Transactional
    public PreferenceResponse updatePreferences(Long userId, PreferenceUpdateRequest request) {
        UserModel user = this.userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setTripType(normalize(request.tripType()));
        user.setInterests(normalizeInterests(request.interests()));
        user.setDestination(normalize(request.destination()));
        if (request.isOnboarded() != null) {
            user.setOnboarded(request.isOnboarded());
        }

        UserModel savedUser = this.userRepository.save(user);
        return toResponse(savedUser);
    }

    private PreferenceResponse toResponse(UserModel user) {
        return new PreferenceResponse(
                user.getId(),
                user.getTripType(),
                normalizeInterests(user.getInterests()),
                user.getDestination(),
                user.getUpdatedAt() != null ? user.getUpdatedAt() : user.getCreatedAt(),
                user.isOnboarded());
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private List<String> normalizeInterests(List<String> interests) {
        if (interests == null || interests.isEmpty()) {
            return List.of();
        }
        return interests.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }
}
