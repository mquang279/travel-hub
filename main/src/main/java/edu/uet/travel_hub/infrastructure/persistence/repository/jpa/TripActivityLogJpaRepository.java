package edu.uet.travel_hub.infrastructure.persistence.repository.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.uet.travel_hub.infrastructure.persistence.entity.TripActivityLogEntity;

public interface TripActivityLogJpaRepository extends JpaRepository<TripActivityLogEntity, Long> {
    List<TripActivityLogEntity> findTop20ByTripIdOrderByCreatedAtDescIdDesc(Long tripId);

    void deleteByTripId(Long tripId);
}