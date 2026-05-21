package edu.uet.travel_hub.application.usecases;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import edu.uet.travel_hub.application.dto.request.CreateTripRequest;
import edu.uet.travel_hub.application.dto.request.UpdateTripRequest;
import edu.uet.travel_hub.application.dto.response.JoinTripResultResponse;
import edu.uet.travel_hub.application.dto.response.TripDashboardResponse;
import edu.uet.travel_hub.application.dto.response.TripDetailHighlightsResponse;
import edu.uet.travel_hub.application.dto.response.TripDetailResponse;
import edu.uet.travel_hub.application.dto.response.TripDetailTopExpenseResponse;
import edu.uet.travel_hub.application.dto.response.TripDetailWinningPollResponse;
import edu.uet.travel_hub.application.dto.response.TripInfoResponse;
import edu.uet.travel_hub.application.dto.response.TripMemberResponse;
import edu.uet.travel_hub.application.exception.ResourceNotFoundException;
import edu.uet.travel_hub.domain.enums.TripRole;
import edu.uet.travel_hub.domain.enums.TripMemberStatus;
import edu.uet.travel_hub.domain.enums.TripMemberRole;
import edu.uet.travel_hub.domain.enums.TripStatus;
import edu.uet.travel_hub.domain.mapper.TripRoleMapper;
import edu.uet.travel_hub.infrastructure.persistence.entity.TripExpenseEntity;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.PollVoteCount;
import edu.uet.travel_hub.infrastructure.persistence.entity.TripEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.TripMemberEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.UserEntity;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.TripExpenseJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.TripJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.TripPollJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.TripPollVoteJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.TripMemberJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.UserJpaRepository;

@Service
public class TripService {
    private static final int DEFAULT_MAX_MEMBERS = 50;

    private final TripJpaRepository tripJpaRepository;
    private final TripMemberJpaRepository tripMemberJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final TripExpenseJpaRepository tripExpenseJpaRepository;
    private final TripPollJpaRepository tripPollJpaRepository;
    private final TripPollVoteJpaRepository tripPollVoteJpaRepository;
    private final edu.uet.travel_hub.application.service.InviteCodeService inviteCodeService;

    public TripService(
            TripJpaRepository tripJpaRepository,
            TripMemberJpaRepository tripMemberJpaRepository,
            UserJpaRepository userJpaRepository,
            TripExpenseJpaRepository tripExpenseJpaRepository,
            TripPollJpaRepository tripPollJpaRepository,
            TripPollVoteJpaRepository tripPollVoteJpaRepository,
            edu.uet.travel_hub.application.service.InviteCodeService inviteCodeService) {
        this.tripJpaRepository = tripJpaRepository;
        this.tripMemberJpaRepository = tripMemberJpaRepository;
        this.userJpaRepository = userJpaRepository;
        this.tripExpenseJpaRepository = tripExpenseJpaRepository;
        this.tripPollJpaRepository = tripPollJpaRepository;
        this.tripPollVoteJpaRepository = tripPollVoteJpaRepository;
        this.inviteCodeService = inviteCodeService;
    }

    public TripEntity findTrip(Long tripId) {
        return this.tripJpaRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
    }

    public edu.uet.travel_hub.infrastructure.persistence.entity.UserEntity findUser(Long userId) {
        return this.userJpaRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
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
            .limit(5)
            .map(this::toMemberResponseWithRole)
            .toList();

        TripDetailHighlightsResponse highlights = buildHighlights(tripId);

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

        TripRole myRole = TripRoleMapper.fromMemberRole(membership.getRole());
    return new TripDetailResponse(tripInfo, myRole.name(), members, highlights);
    }

    @Transactional(readOnly = true)
    public edu.uet.travel_hub.application.dto.response.TripInviteCodeResponse getInviteCode(Long tripId, Long currentUserId) {
        TripEntity trip = requireActiveMemberTrip(tripId, currentUserId);
        String code = trip.getInviteCode();
        String link = "tripapp://join?code=" + code;
        return new edu.uet.travel_hub.application.dto.response.TripInviteCodeResponse(code, link, trip.getInviteCodeExpiredAt());
    }

    @Transactional
    public edu.uet.travel_hub.application.dto.response.TripInviteCodeResponse regenerateInviteCode(Long tripId, Long currentUserId) {
        TripEntity trip = requireLeaderTrip(tripId, currentUserId);
        String newCode = this.inviteCodeService.generateInviteCode();
        trip.setInviteCode(newCode);
        trip.setInviteCodeExpiredAt(null);
        TripEntity saved = this.tripJpaRepository.save(trip);
        String link = "tripapp://join?code=" + newCode;
        return new edu.uet.travel_hub.application.dto.response.TripInviteCodeResponse(newCode, link, saved.getInviteCodeExpiredAt());
    }

    @Transactional
    public Long createTrip(Long currentUserId, CreateTripRequest request) {
        // Validate dates
        LocalDate startDate = request.startDate();
        LocalDate endDate = request.endDate();
        LocalDate today = LocalDate.now();

        // Check start date >= today
        if (startDate.isBefore(today)) {
            throw new IllegalArgumentException("Ngày đi phải lớn hơn hoặc bằng ngày hôm nay");
        }

        // Check end date >= start date
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Ngày kết thúc phải lớn hơn hoặc bằng ngày đi");
        }

        // Check end date <= start date + 60 days
        if (endDate.isAfter(startDate.plusDays(60))) {
            throw new IllegalArgumentException("Ngày kết thúc không được vượt quá 60 ngày từ ngày đi");
        }

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
            .inviteCode(this.inviteCodeService.generateInviteCode())
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

    @Transactional
    public void updateTrip(Long tripId, Long currentUserId, UpdateTripRequest request) {
        TripEntity trip = requireLeaderTrip(tripId, currentUserId);
        
        // Validate dates if they are being updated
        LocalDate startDate = request.startDate();
        LocalDate endDate = request.endDate();
        LocalDate today = LocalDate.now();

        // Check start date >= today
        if (startDate.isBefore(today)) {
            throw new IllegalArgumentException("Ngày đi phải lớn hơn hoặc bằng ngày hôm nay");
        }

        // Check end date >= start date
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Ngày kết thúc phải lớn hơn hoặc bằng ngày đi");
        }

        // Check end date <= start date + 60 days
        if (endDate.isAfter(startDate.plusDays(60))) {
            throw new IllegalArgumentException("Ngày kết thúc không được vượt quá 60 ngày từ ngày đi");
        }

        trip.setName(normalizeRequired(request.name(), "name"));
        trip.setLocation(normalizeRequired(request.destination(), "destination"));
        trip.setStartDate(request.startDate());
        trip.setEndDate(request.endDate());
        trip.setBudgetMin(request.budgetMin());
        trip.setBudgetMax(request.budgetMax());
        this.tripJpaRepository.save(trip);
    }

    @Transactional
    public void deleteTrip(Long tripId, Long currentUserId) {
        TripEntity trip = requireLeaderTrip(tripId, currentUserId);
        this.tripPollVoteJpaRepository.deleteVotesByTripId(tripId);
        this.tripExpenseJpaRepository.deleteByTripId(tripId);
        this.tripPollJpaRepository.deleteByTripId(tripId);
        // Delete trip (members will cascade delete)
        this.tripJpaRepository.delete(trip);
    }

    @Transactional
    public JoinTripResultResponse joinByInviteCode(Long currentUserId, String rawInviteCode) {
        String inviteCode = normalizeRequired(rawInviteCode, "inviteCode").toUpperCase(Locale.ROOT);
        TripEntity trip = this.tripJpaRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new IllegalArgumentException("Mã mời không hợp lệ"));
        UserEntity currentUser = findUser(currentUserId);

        if (trip.getInviteCodeExpiredAt() != null && trip.getInviteCodeExpiredAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Mã mời đã hết hạn");
        }

        TripMemberEntity member = this.tripMemberJpaRepository.findByTripIdAndUserId(trip.getId(), currentUserId)
                .orElseGet(() -> TripMemberEntity.builder()
                        .trip(trip)
                        .user(currentUser)
                        .role(TripMemberRole.MEMBER)
                        .status(TripMemberStatus.PENDING)
                        .build());

        if (member.getStatus() == TripMemberStatus.ACTIVE) {
            throw new DataIntegrityViolationException("Bạn đã là thành viên của chuyến đi này");
        }

        if (member.getStatus() == TripMemberStatus.PENDING) {
            throw new DataIntegrityViolationException("Yêu cầu tham gia đã được gửi");
        }

        member.setStatus(TripMemberStatus.PENDING);
        member.setRole(TripMemberRole.MEMBER);
        member.setRequestedAt(java.time.Instant.now());
        member.setRespondedAt(null);
        TripMemberEntity savedMember = this.tripMemberJpaRepository.save(member);
        return new JoinTripResultResponse(trip.getId(), savedMember.getStatus().name(), "Yêu cầu tham gia đã được gửi, chờ trưởng nhóm phê duyệt");
    }

    // invite code generation delegated to InviteCodeService

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
                member.getUser().getAvatarUrl(),
                member.getRole() == null ? null : member.getRole().name());
    }

    private TripMemberResponse toMemberResponseWithRole(TripMemberEntity member) {
        return toMemberResponse(member);
    }

    private TripDetailHighlightsResponse buildHighlights(Long tripId) {
        TripDetailTopExpenseResponse topExpense = this.tripExpenseJpaRepository.findByTripIdOrderByExpenseDateDescIdDesc(tripId)
                .stream()
                .max(Comparator.comparing(TripExpenseEntity::getAmount))
                .map(expense -> new TripDetailTopExpenseResponse(expense.getTitle(), expense.getAmount()))
                .orElse(null);

        Map<Long, Long> voteCounts = this.tripPollVoteJpaRepository.countVotesByPollForTrip(tripId).stream()
                .collect(Collectors.toMap(PollVoteCount::getPollId, PollVoteCount::getCount));
        TripDetailWinningPollResponse winningPoll = this.tripPollJpaRepository.findByTripIdOrderByCreatedAtDesc(tripId).stream()
                .map(poll -> Map.entry(poll, voteCounts.getOrDefault(poll.getId(), 0L)))
                .filter(entry -> entry.getValue() > 0)
                .max(Map.Entry.comparingByValue())
                .map(entry -> new TripDetailWinningPollResponse(entry.getKey().getTitle(), entry.getValue().intValue()))
                .orElse(null);

        return new TripDetailHighlightsResponse(topExpense, winningPoll);
    }

    private String displayName(UserEntity user) {
        if (user.getName() != null && !user.getName().isBlank()) {
            return user.getName();
        }
        return user.getUsername();
    }

    private String formatDate(LocalDate value) {
        return value == null ? null : DISPLAY_DATE_FORMATTER.format(value);
    }

    private String buildDateRange(LocalDate start, LocalDate end) {
        if (start == null && end == null) {
            return "Unknown";
        }
        if (start == null) {
            return formatDate(end);
        }
        if (end == null || end.equals(start)) {
            return formatDate(start);
        }
        return formatDate(start) + " - " + formatDate(end);
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