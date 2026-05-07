package edu.uet.travel_hub.infrastructure.persistence.repository.impl;

import java.time.Instant;

import org.springframework.stereotype.Repository;

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
    public void add(String token, Long userId) {
        UserEntity user = this.userJpaRepository.findById(userId).get();
        DeviceTokenEntity entity = DeviceTokenEntity.builder()
                .token(token)
                .user(user)
                .build();
        this.deviceTokenJpaRepository.save(entity);
    }

}
