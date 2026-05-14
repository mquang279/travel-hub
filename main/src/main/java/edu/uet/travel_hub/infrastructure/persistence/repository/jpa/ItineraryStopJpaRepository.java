package edu.uet.travel_hub.infrastructure.persistence.repository.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.uet.travel_hub.infrastructure.persistence.entity.ItineraryStopEntity;

public interface ItineraryStopJpaRepository extends JpaRepository<ItineraryStopEntity, Long> {
    Optional<ItineraryStopEntity> findByIdAndDayItineraryId(Long id, Long itineraryId);
}
