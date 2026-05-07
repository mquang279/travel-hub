package edu.uet.travel_hub.application.usecases;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.uet.travel_hub.application.dto.request.CreateTripRequest;
import edu.uet.travel_hub.application.dto.response.TripDashboardResponse;
import edu.uet.travel_hub.domain.enums.TripRole;
import edu.uet.travel_hub.domain.enums.TripMemberRole;
import edu.uet.travel_hub.domain.enums.TripStatus;
import edu.uet.travel_hub.infrastructure.persistence.entity.TripEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.TripMemberEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.UserEntity;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.TripJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.TripMemberJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.UserJpaRepository;

@Service
public class TripService {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final TripJpaRepository tripJpaRepository;
    private final TripMemberJpaRepository tripMemberJpaRepository;
    private final UserJpaRepository userJpaRepository;

    public TripService(
            TripJpaRepository tripJpaRepository,
            TripMemberJpaRepository tripMemberJpaRepository,
            UserJpaRepository userJpaRepository) {
        this.tripJpaRepository = tripJpaRepository;
        this.tripMemberJpaRepository = tripMemberJpaRepository;
        this.userJpaRepository = userJpaRepository;
    }

    public TripEntity findTrip(Long tripId) {
        return this.tripJpaRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found"));
    }

    public edu.uet.travel_hub.infrastructure.persistence.entity.UserEntity findUser(Long userId) {
        return this.userJpaRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public TripEntity requireActiveMemberTrip(Long tripId, Long currentUserId) {
        TripEntity trip = findTrip(tripId);
        boolean active = this.tripMemberJpaRepository.findByTripIdAndUserId(tripId, currentUserId)
                .map(m -> m.getStatus() == edu.uet.travel_hub.domain.enums.TripMemberStatus.ACTIVE)
                .orElse(false);
        if (!active) {
            throw new edu.uet.travel_hub.application.exception.ForbiddenTripActionException("User is not an active member");
        }
        return trip;
    }

    public TripEntity requireLeaderTrip(Long tripId, Long currentUserId) {
        TripEntity trip = findTrip(tripId);
        if (!trip.getLeader().getId().equals(currentUserId)) {
            throw new edu.uet.travel_hub.application.exception.ForbiddenTripActionException("Only leader can perform this action");
        }
        return trip;
    }

    @Transactional(readOnly = true)
    public TripDashboardResponse getDashboard(Long currentUserId) {
        this.tripJpaRepository.findDistinctByMembersUserIdOrderByStartDateAsc(currentUserId);
        return new TripDashboardResponse(null, List.of(), List.of());
    }

    @Transactional
    public Long createTrip(Long currentUserId, CreateTripRequest request) {
        UserEntity leader = this.userJpaRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        TripEntity trip = TripEntity.builder()
                .name(request.name())
                .location(request.destination())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .budgetMin(request.budgetMin() == null ? null : BigDecimal.valueOf(request.budgetMin()))
                .budgetMax(request.budgetMax() == null ? null : BigDecimal.valueOf(request.budgetMax()))
                .leader(leader)
                .inviteCode(generateInviteCode())
                .status(TripStatus.fromDates(request.startDate(), request.endDate()))
                .build();

        TripEntity saved = this.tripJpaRepository.save(trip);
        this.tripMemberJpaRepository.save(TripMemberEntity.builder()
            .trip(saved)
            .user(leader)
            .role(TripMemberRole.LEADER)
            .status(edu.uet.travel_hub.domain.enums.TripMemberStatus.ACTIVE)
            .build());
        return saved.getId();
    }

    private String generateInviteCode() {
        int value = 100000 + RANDOM.nextInt(900000);
        return "TRIP" + value;
    }
}