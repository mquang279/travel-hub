package edu.uet.travel_hub.infrastructure.persistence.repository.jpa;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import edu.uet.travel_hub.infrastructure.persistence.entity.TripDayEntity;

public interface TripDayJpaRepository extends JpaRepository<TripDayEntity, Long> {
    @EntityGraph(attributePaths = {"activities"})
    List<TripDayEntity> findByTripIdOrderByDateAscIdAsc(Long tripId);

    Optional<TripDayEntity> findByTripIdAndDate(Long tripId, LocalDate date);

    Optional<TripDayEntity> findByIdAndTripId(Long id, Long tripId);
}
