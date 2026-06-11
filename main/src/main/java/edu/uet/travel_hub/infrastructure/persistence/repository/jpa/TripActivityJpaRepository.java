package edu.uet.travel_hub.infrastructure.persistence.repository.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.uet.travel_hub.infrastructure.persistence.entity.TripActivityEntity;

public interface TripActivityJpaRepository extends JpaRepository<TripActivityEntity, Long> {
    Optional<TripActivityEntity> findByIdAndTripDayTripId(Long id, Long tripId);

    long countByTripDayId(Long tripDayId);

    List<TripActivityEntity> findByTripDayTripIdOrderByTripDayDateAscTripDayIdAscOrderIndexAscIdAsc(Long tripId);
}
