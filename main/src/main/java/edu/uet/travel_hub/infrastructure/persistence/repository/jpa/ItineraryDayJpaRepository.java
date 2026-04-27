package edu.uet.travel_hub.infrastructure.persistence.repository.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.uet.travel_hub.infrastructure.persistence.entity.ItineraryDayEntity;

public interface ItineraryDayJpaRepository extends JpaRepository<ItineraryDayEntity, Long> {
    Optional<ItineraryDayEntity> findByIdAndItineraryId(Long id, Long itineraryId);
}
