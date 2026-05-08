package edu.uet.travel_hub.infrastructure.persistence.repository.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.uet.travel_hub.domain.enums.TripMemberRole;
import edu.uet.travel_hub.domain.enums.TripMemberStatus;
import edu.uet.travel_hub.infrastructure.persistence.entity.TripMemberEntity;

import java.util.List;


public interface TripMemberJpaRepository extends JpaRepository<TripMemberEntity, Long> {
    Optional<TripMemberEntity> findByTripIdAndUserId(Long tripId, Long userId);

    boolean existsByTripIdAndUserId(Long tripId, Long userId);

    List<TripMemberEntity> findByTripIdAndStatus(Long tripId, TripMemberStatus status);

    long countByTripId(Long tripId);

    long countByTripIdAndStatus(Long tripId, TripMemberStatus status);

    long countByTripIdAndRole(Long tripId, TripMemberRole role);

    List<TripMemberEntity> findByTripIdAndStatusOrderByRequestedAtAsc(Long tripId, TripMemberStatus status);
}