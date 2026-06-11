package edu.uet.travel_hub.infrastructure.persistence.repository.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.uet.travel_hub.infrastructure.persistence.entity.SettlementEntity;

public interface SettlementJpaRepository extends JpaRepository<SettlementEntity, Long> {
    List<SettlementEntity> findByTripIdOrderByIdAsc(Long tripId);

    Optional<SettlementEntity> findByIdAndTripId(Long id, Long tripId);

    boolean existsByTripId(Long tripId);
}
