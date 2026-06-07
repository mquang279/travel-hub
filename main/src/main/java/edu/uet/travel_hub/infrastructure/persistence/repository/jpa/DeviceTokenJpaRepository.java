package edu.uet.travel_hub.infrastructure.persistence.repository.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.uet.travel_hub.infrastructure.persistence.entity.DeviceTokenEntity;

public interface DeviceTokenJpaRepository extends JpaRepository<DeviceTokenEntity, Long> {
    List<DeviceTokenEntity> findByUserId(Long userId);

    Optional<DeviceTokenEntity> findByToken(String token);
}
