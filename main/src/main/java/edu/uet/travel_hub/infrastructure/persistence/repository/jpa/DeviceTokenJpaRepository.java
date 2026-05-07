package edu.uet.travel_hub.infrastructure.persistence.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.uet.travel_hub.infrastructure.persistence.entity.DeviceTokenEntity;

public interface DeviceTokenJpaRepository extends JpaRepository<DeviceTokenEntity, Long> {

}
