package edu.uet.travel_hub.infrastructure.persistence.repository.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.uet.travel_hub.infrastructure.persistence.entity.TripPhotoEntity;

public interface TripPhotoJpaRepository extends JpaRepository<TripPhotoEntity, Long> {
    List<TripPhotoEntity> findByTripIdOrderByUploadedAtDescIdDesc(Long tripId);
    Optional<TripPhotoEntity> findByIdAndTripId(Long id, Long tripId);
}
