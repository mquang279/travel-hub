package edu.uet.travel_hub.infrastructure.persistence.repository.impl;

import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import edu.uet.travel_hub.application.port.out.TripLookupRepository;
import edu.uet.travel_hub.domain.enums.TripMemberStatus;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.TripJpaRepository;

@Repository
public class TripLookupRepositoryImpl implements TripLookupRepository {
    private final TripJpaRepository tripJpaRepository;

    public TripLookupRepositoryImpl(TripJpaRepository tripJpaRepository) {
        this.tripJpaRepository = tripJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findActiveMemberTripNameById(Long tripId, Long userId, TripMemberStatus status) {
        return this.tripJpaRepository.findActiveMemberTripById(tripId, userId, status).map(trip -> trip.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findFirstActiveMemberTripNameByName(Long userId, TripMemberStatus status, String name) {
        return this.tripJpaRepository.findActiveMemberTripsByName(userId, status, name)
                .stream()
                .findFirst()
                .map(trip -> trip.getName());
    }
}
