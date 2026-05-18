package edu.uet.travel_hub.infrastructure.persistence.repository.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.uet.travel_hub.infrastructure.persistence.entity.TripPollEntity;

public interface TripPollJpaRepository extends JpaRepository<TripPollEntity, Long> {
    List<TripPollEntity> findByTripIdOrderByCreatedAtDesc(Long tripId);

    Optional<TripPollEntity> findByIdAndTripId(Long id, Long tripId);

    void deleteByTripId(Long tripId);
}