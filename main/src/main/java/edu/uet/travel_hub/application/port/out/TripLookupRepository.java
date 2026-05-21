package edu.uet.travel_hub.application.port.out;

import java.util.Optional;

import edu.uet.travel_hub.domain.enums.TripMemberStatus;

public interface TripLookupRepository {
    Optional<String> findActiveMemberTripNameById(Long tripId, Long userId, TripMemberStatus status);

    Optional<String> findFirstActiveMemberTripNameByName(Long userId, TripMemberStatus status, String name);
}
