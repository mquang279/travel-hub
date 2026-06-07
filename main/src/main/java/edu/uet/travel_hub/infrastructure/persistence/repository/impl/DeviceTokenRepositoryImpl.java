package edu.uet.travel_hub.infrastructure.persistence.repository.impl;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import edu.uet.travel_hub.application.port.out.DeviceTokenRepository;
import edu.uet.travel_hub.infrastructure.persistence.entity.DeviceTokenEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.UserEntity;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.DeviceTokenJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.UserJpaRepository;
import lombok.AllArgsConstructor;

@Repository
@AllArgsConstructor
public class DeviceTokenRepositoryImpl implements DeviceTokenRepository {
    private final DeviceTokenJpaRepository deviceTokenJpaRepository;
    private final UserJpaRepository userJpaRepository;

    @Override
    @Transactional
    public void add(String token, Long userId) {
        String normalizedToken = token == null ? "" : token.trim();
        if (normalizedToken.isEmpty()) {
            return;
        }

        UserEntity user = this.userJpaRepository.findById(userId).get();

        this.deviceTokenJpaRepository.findByToken(normalizedToken).ifPresentOrElse(existingToken -> {
            if (!existingToken.getUser().getId().equals(userId)) {
                existingToken.setUser(user);
            }
            this.deviceTokenJpaRepository.save(existingToken);
        }, () -> {
            DeviceTokenEntity entity = DeviceTokenEntity.builder()
                    .token(normalizedToken)
                    .user(user)
                    .build();
            this.deviceTokenJpaRepository.save(entity);
        });
    }

}
