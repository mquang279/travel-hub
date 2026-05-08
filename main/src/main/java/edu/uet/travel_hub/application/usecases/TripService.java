package edu.uet.travel_hub.application.usecases;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.uet.travel_hub.application.dto.request.CreateTripRequest;
import edu.uet.travel_hub.application.dto.request.UpdateTripRequest;
import edu.uet.travel_hub.application.dto.response.JoinTripResultResponse;
import edu.uet.travel_hub.application.dto.response.TripActivityLogResponse;
import edu.uet.travel_hub.application.dto.response.TripDashboardResponse;
import edu.uet.travel_hub.application.dto.response.TripDetailResponse;
import edu.uet.travel_hub.application.dto.response.TripHighlightResponse;
import edu.uet.travel_hub.application.dto.response.TripInfoResponse;
import edu.uet.travel_hub.application.dto.response.TripMemberResponse;
import edu.uet.travel_hub.application.dto.response.TripMembersResponse;
import edu.uet.travel_hub.application.exception.ResourceNotFoundException;
import edu.uet.travel_hub.domain.enums.TripRole;
import edu.uet.travel_hub.domain.enums.TripMemberStatus;
import edu.uet.travel_hub.domain.enums.TripMemberRole;
import edu.uet.travel_hub.domain.enums.TripStatus;
import edu.uet.travel_hub.domain.mapper.TripRoleMapper;
import edu.uet.travel_hub.infrastructure.persistence.entity.TripEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.TripMemberEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.UserEntity;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.TripActivityLogJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.TripJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.TripMemberJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.UserJpaRepository;

@Service
public class TripService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int DEFAULT_MAX_MEMBERS = 50;

    private final TripJpaRepository tripJpaRepository;
    private final TripMemberJpaRepository tripMemberJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final TripActivityLogJpaRepository tripActivityLogJpaRepository;
    private final TripActivityLogService tripActivityLogService;

    public TripService(
            TripJpaRepository tripJpaRepository,
            TripMemberJpaRepository tripMemberJpaRepository,
            UserJpaRepository userJpaRepository,
            TripActivityLogJpaRepository tripActivityLogJpaRepository,
            TripActivityLogService tripActivityLogService) {
        this.tripJpaRepository = tripJpaRepository;
        this.tripMemberJpaRepository = tripMemberJpaRepository;
        this.userJpaRepository = userJpaRepository;
        this.tripActivityLogJpaRepository = tripActivityLogJpaRepository;
        this.tripActivityLogService = tripActivityLogService;
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
        LocalDate today = LocalDate.now();
        List<TripEntity> memberTrips = this.tripJpaRepository.findDistinctByMembersUserIdOrderByStartDateAsc(currentUserId)
            .stream()
            .filter(trip -> isActiveMember(trip, currentUserId))
            .toList();

        TripDashboardResponse.ActiveTripResponse activeTrip = memberTrips.stream()
            .filter(trip -> trip.getStatus() == TripStatus.ONGOING)
            .findFirst()
            .map(this::toActiveTrip)
            .orElse(null);

        List<TripDashboardResponse.UpcomingTripResponse> upcomingTrips = memberTrips.stream()
            .filter(trip -> trip.getStatus() == TripStatus.UPCOMING || trip.getStatus() == TripStatus.PLANNING)
            .sorted((left, right) -> compareNullableDates(left.getStartDate(), right.getStartDate()))
            .map(trip -> toUpcomingTrip(trip, today))
            .toList();

        List<TripDashboardResponse.PastTripResponse> pastTrips = memberTrips.stream()
            .filter(trip -> trip.getStatus() == TripStatus.COMPLETED)
            .sorted((left, right) -> compareNullableDatesDesc(left.getEndDate(), right.getEndDate()))
            .map(this::toPastTrip)
            .toList();

        return new TripDashboardResponse(activeTrip, upcomingTrips, pastTrips);
    }

    @Transactional(readOnly = true)
    public TripDetailResponse getTripDetail(Long tripId, Long currentUserId) {
        TripEntity trip = requireActiveMemberTrip(tripId, currentUserId);
        TripMemberEntity membership = this.tripMemberJpaRepository.findByTripIdAndUserId(tripId, currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Trip member not found"));

        List<TripMemberEntity> activeMembers = this.tripMemberJpaRepository.findByTripIdAndStatusOrderByRequestedAtAsc(
            tripId,
            TripMemberStatus.ACTIVE);

        List<TripMemberResponse> members = activeMembers.stream()
            .map(this::toMemberResponse)
            .toList();

        List<TripHighlightResponse> highlights = activeMembers.stream()
            .limit(5)
            .map(member -> new TripHighlightResponse(
                displayName(member.getUser()),
                member.getUser().getAvatarUrl()))
            .toList();

        List<TripActivityLogResponse> recentActivities = this.tripActivityLogJpaRepository
            .findTop20ByTripIdOrderByCreatedAtDescIdDesc(tripId)
            .stream()
            .map(log -> new TripActivityLogResponse(
                log.getId(),
                log.getActionType(),
                log.getTargetType(),
                log.getTargetId(),
                log.getDescription(),
                log.getCreatedAt()))
            .toList();

        TripInfoResponse tripInfo = new TripInfoResponse(
            trip.getId(),
            trip.getName(),
            trip.getLocation(),
            trip.getCoverImageUrl(),
            trip.getDescription(),
            trip.getStartDate(),
            trip.getEndDate(),
            trip.getBudgetMin(),
            trip.getBudgetMax(),
            trip.getStatus(),
            trip.getInviteCode(),
            DEFAULT_MAX_MEMBERS);

        TripMembersResponse membersResponse = new TripMembersResponse(
            members.size(),
            DEFAULT_MAX_MEMBERS,
            members);

        TripRole myRole = TripRoleMapper.fromMemberRole(membership.getRole());
        return new TripDetailResponse(tripInfo, myRole.name(), membersResponse, highlights, recentActivities);
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

        this.tripActivityLogService.log(saved, leader, "CREATE_TRIP", "TRIP", saved.getId(), "trip created");
        return saved.getId();
    }

    @Transactional
    public void updateTrip(Long tripId, Long currentUserId, UpdateTripRequest request) {
        TripEntity trip = requireLeaderTrip(tripId, currentUserId);
        trip.setName(normalizeRequired(request.name(), "name"));
        trip.setLocation(normalizeRequired(request.destination(), "destination"));
        trip.setStartDate(request.startDate());
        trip.setEndDate(request.endDate());
        trip.setBudgetMin(request.budgetMin());
        trip.setBudgetMax(request.budgetMax());
        this.tripJpaRepository.save(trip);
        this.tripActivityLogService.log(trip, trip.getLeader(), "UPDATE_TRIP", "TRIP", tripId, "trip updated");
    }

    @Transactional
    public void deleteTrip(Long tripId, Long currentUserId) {
        TripEntity trip = requireLeaderTrip(tripId, currentUserId);
        this.tripJpaRepository.delete(trip);
    }

    @Transactional
    public JoinTripResultResponse joinByInviteCode(Long currentUserId, String rawInviteCode) {
        String inviteCode = normalizeRequired(rawInviteCode, "inviteCode").toUpperCase(Locale.ROOT);
        TripEntity trip = this.tripJpaRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new ResourceNotFoundException("Invite code is invalid"));
        UserEntity currentUser = findUser(currentUserId);

        TripMemberEntity member = this.tripMemberJpaRepository.findByTripIdAndUserId(trip.getId(), currentUserId)
                .orElseGet(() -> this.tripMemberJpaRepository.save(TripMemberEntity.builder()
                        .trip(trip)
                        .user(currentUser)
                        .role(TripMemberRole.MEMBER)
                        .status(TripMemberStatus.PENDING)
                        .build()));

        if (member.getStatus() == TripMemberStatus.ACTIVE) {
            return new JoinTripResultResponse(trip.getId(), member.getStatus().name(), "Already a member");
        }

        if (member.getStatus() == TripMemberStatus.PENDING) {
            return new JoinTripResultResponse(trip.getId(), member.getStatus().name(), "Join request already pending");
        }

        member.setStatus(TripMemberStatus.PENDING);
        member.setRole(TripMemberRole.MEMBER);
        member.setRequestedAt(java.time.Instant.now());
        member.setRespondedAt(null);
        this.tripMemberJpaRepository.save(member);
        this.tripActivityLogService.log(trip, currentUser, "REQUEST_JOIN", "USER", currentUserId, "join request submitted");
        return new JoinTripResultResponse(trip.getId(), member.getStatus().name(), "Join request submitted");
    }

    private String generateInviteCode() {
        int value = 100000 + RANDOM.nextInt(900000);
        return "TRIP" + value;
    }

    private boolean isActiveMember(TripEntity trip, Long currentUserId) {
        return trip.getMembers().stream().anyMatch(member ->
                member.getUser() != null
                        && currentUserId.equals(member.getUser().getId())
                        && member.getStatus() == TripMemberStatus.ACTIVE);
    }

    private TripDashboardResponse.ActiveTripResponse toActiveTrip(TripEntity trip) {
        return new TripDashboardResponse.ActiveTripResponse(
                trip.getId(),
                trip.getName(),
                trip.getLocation(),
                trip.getCoverImageUrl(),
                formatDate(trip.getStartDate()),
                formatDate(trip.getEndDate()));
    }

    private TripDashboardResponse.UpcomingTripResponse toUpcomingTrip(TripEntity trip, LocalDate today) {
        int daysLeft = 0;
        if (trip.getStartDate() != null) {
            daysLeft = (int) Math.max(0, ChronoUnit.DAYS.between(today, trip.getStartDate()));
        }
        int memberCount = (int) trip.getMembers().stream()
                .filter(member -> member.getStatus() == TripMemberStatus.ACTIVE)
                .count();
        return new TripDashboardResponse.UpcomingTripResponse(
                trip.getId(),
                trip.getName(),
                trip.getLocation(),
                trip.getCoverImageUrl(),
                daysLeft,
                memberCount);
    }

    private TripDashboardResponse.PastTripResponse toPastTrip(TripEntity trip) {
        return new TripDashboardResponse.PastTripResponse(
                trip.getId(),
                trip.getLocation(),
                buildDateRange(trip.getStartDate(), trip.getEndDate()),
                trip.getCoverImageUrl());
    }

    private TripMemberResponse toMemberResponse(TripMemberEntity member) {
        return new TripMemberResponse(
                member.getUser().getId(),
                displayName(member.getUser()),
                member.getUser().getAvatarUrl());
    }

    private String displayName(UserEntity user) {
        if (user.getName() != null && !user.getName().isBlank()) {
            return user.getName();
        }
        return user.getUsername();
    }

    private String formatDate(LocalDate value) {
        return value == null ? null : value.toString();
    }

    private String buildDateRange(LocalDate start, LocalDate end) {
        if (start == null && end == null) {
            return "Unknown";
        }
        if (start == null) {
            return end.toString();
        }
        if (end == null || end.equals(start)) {
            return start.toString();
        }
        return start + " - " + end;
    }

    private int compareNullableDates(LocalDate left, LocalDate right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return left.compareTo(right);
    }

    private int compareNullableDatesDesc(LocalDate left, LocalDate right) {
        return compareNullableDates(right, left);
    }

    private String normalizeRequired(String value, String fieldName) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return normalized;
    }
}