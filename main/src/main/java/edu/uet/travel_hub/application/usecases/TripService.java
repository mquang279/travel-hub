package edu.uet.travel_hub.application.usecases;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import edu.uet.travel_hub.application.dto.request.CreateTripRequest;
import edu.uet.travel_hub.application.dto.request.CreateTripPostRequest;
import edu.uet.travel_hub.application.dto.request.UpdateTripRequest;
import edu.uet.travel_hub.application.dto.response.JoinTripResultResponse;
import edu.uet.travel_hub.application.dto.response.PaginationResponse;
import edu.uet.travel_hub.application.dto.response.TripPhotoResponse;
import edu.uet.travel_hub.application.dto.response.TripActivityItemResponse;
import edu.uet.travel_hub.application.dto.response.TripActivityLogResponse;
import edu.uet.travel_hub.application.dto.response.TripDashboardResponse;
import edu.uet.travel_hub.application.dto.response.TripDetailHighlightsResponse;
import edu.uet.travel_hub.application.dto.response.TripDetailResponse;
import edu.uet.travel_hub.application.dto.response.TripDetailTopExpenseResponse;
import edu.uet.travel_hub.application.dto.response.TripDetailWinningPollResponse;
import edu.uet.travel_hub.application.dto.response.TripHighlightResponse;
import edu.uet.travel_hub.application.dto.response.TripInfoResponse;
import edu.uet.travel_hub.application.dto.response.TripMemberResponse;
import edu.uet.travel_hub.application.exception.ResourceNotFoundException;
import edu.uet.travel_hub.domain.enums.TripRole;
import edu.uet.travel_hub.domain.enums.TripMemberStatus;
import edu.uet.travel_hub.domain.enums.TripMemberRole;
import edu.uet.travel_hub.domain.enums.TripStatus;
import edu.uet.travel_hub.domain.mapper.TripRoleMapper;
import edu.uet.travel_hub.domain.enums.TripExpenseCategory;
import edu.uet.travel_hub.infrastructure.persistence.entity.TripExpenseEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.TripPollEntity;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.PollVoteCount;
import edu.uet.travel_hub.infrastructure.persistence.entity.TripEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.TripMemberEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.TripPhotoEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.UserEntity;
import edu.uet.travel_hub.domain.model.PostModel;
import edu.uet.travel_hub.application.port.out.PostRepository;
import edu.uet.travel_hub.application.port.out.UserRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.TripExpenseJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.TripActivityLogJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.TripJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.TripPollJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.TripPollVoteJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.TripMemberJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.TripPhotoJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.TravelPlaceImageJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.UserJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.BankAccountJpaRepository;

@Service
public class TripService {
    private static final int DEFAULT_MAX_MEMBERS = 50;

    private final TripJpaRepository tripJpaRepository;
    private final TripMemberJpaRepository tripMemberJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final TripExpenseJpaRepository tripExpenseJpaRepository;
    private final TripPollJpaRepository tripPollJpaRepository;
    private final TripPollVoteJpaRepository tripPollVoteJpaRepository;
    private final TripActivityLogJpaRepository tripActivityLogJpaRepository;
    private final TripPhotoJpaRepository tripPhotoJpaRepository;
    private final TravelPlaceImageJpaRepository travelPlaceImageJpaRepository;
    private final BankAccountJpaRepository bankAccountJpaRepository;
    private final TripActivityLogService tripActivityLogService;
    private final edu.uet.travel_hub.application.service.InviteCodeService inviteCodeService;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public TripService(
            TripJpaRepository tripJpaRepository,
            TripMemberJpaRepository tripMemberJpaRepository,
            UserJpaRepository userJpaRepository,
            TripExpenseJpaRepository tripExpenseJpaRepository,
            TripPollJpaRepository tripPollJpaRepository,
            TripPollVoteJpaRepository tripPollVoteJpaRepository,
            TripActivityLogJpaRepository tripActivityLogJpaRepository,
            TripPhotoJpaRepository tripPhotoJpaRepository,
            TravelPlaceImageJpaRepository travelPlaceImageJpaRepository,
            BankAccountJpaRepository bankAccountJpaRepository,
            TripActivityLogService tripActivityLogService,
            edu.uet.travel_hub.application.service.InviteCodeService inviteCodeService,
            PostRepository postRepository,
            UserRepository userRepository) {
        this.tripJpaRepository = tripJpaRepository;
        this.tripMemberJpaRepository = tripMemberJpaRepository;
        this.userJpaRepository = userJpaRepository;
        this.tripExpenseJpaRepository = tripExpenseJpaRepository;
        this.tripPollJpaRepository = tripPollJpaRepository;
        this.tripPollVoteJpaRepository = tripPollVoteJpaRepository;
        this.tripActivityLogJpaRepository = tripActivityLogJpaRepository;
        this.tripPhotoJpaRepository = tripPhotoJpaRepository;
        this.travelPlaceImageJpaRepository = travelPlaceImageJpaRepository;
        this.bankAccountJpaRepository = bankAccountJpaRepository;
        this.tripActivityLogService = tripActivityLogService;
        this.inviteCodeService = inviteCodeService;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
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
            .filter(trip -> resolveDashboardStatus(trip) == TripStatus.ONGOING)
            .findFirst()
            .map(this::toActiveTrip)
            .orElse(null);

        List<TripDashboardResponse.UpcomingTripResponse> upcomingTrips = memberTrips.stream()
            .filter(trip -> {
                TripStatus status = resolveDashboardStatus(trip);
                return status == TripStatus.UPCOMING || status == TripStatus.PLANNING;
            })
            .sorted((left, right) -> compareNullableDates(left.getStartDate(), right.getStartDate()))
            .map(trip -> toUpcomingTrip(trip, today))
            .toList();

        return new TripDashboardResponse(activeTrip, upcomingTrips, List.of());
    }

    @Transactional(readOnly = true)
    public PaginationResponse<TripDashboardResponse.PastTripResponse> getPastTrips(
            Long currentUserId,
            int page,
            int pageSize) {
        int safePage = Math.max(0, page);
        int safePageSize = Math.min(Math.max(1, pageSize), 50);
        List<TripDashboardResponse.PastTripResponse> pastTrips = this.tripJpaRepository
            .findDistinctByMembersUserIdOrderByStartDateAsc(currentUserId)
            .stream()
            .filter(trip -> isActiveMember(trip, currentUserId))
            .filter(trip -> resolveDashboardStatus(trip) == TripStatus.COMPLETED)
            .sorted((left, right) -> compareNullableDatesDesc(left.getEndDate(), right.getEndDate()))
            .map(this::toPastTrip)
            .toList();

        int totalElements = pastTrips.size();
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / safePageSize);
        int fromIndex = safePage * safePageSize;
        List<TripDashboardResponse.PastTripResponse> pageData = fromIndex >= totalElements
                ? List.of()
                : pastTrips.subList(fromIndex, Math.min(fromIndex + safePageSize, totalElements));

        return new PaginationResponse<>(
                safePage,
                safePageSize,
                totalPages,
                (long) totalElements,
                pageData);
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
            .map(this::toMemberResponseWithRole)
            .toList();

        TripDetailHighlightsResponse highlights = buildHighlights(tripId);

        List<TripActivityItemResponse> recentActivities = this.tripActivityLogJpaRepository
            .findTop20ByTripIdOrderByCreatedAtDescIdDesc(tripId)
            .stream()
            .map(this::toActivityItem)
            .filter(item -> item != null)
            .limit(5)
            .toList();

        TripInfoResponse tripInfo = new TripInfoResponse(
            trip.getId(),
            trip.getName(),
            trip.getLocation(),
            trip.getCoverImageUrl(),
            trip.getPlaceId(),
            trip.getDescription(),
            trip.getStartDate(),
            trip.getEndDate(),
            trip.getBudgetMin(),
            trip.getBudgetMax(),
            trip.getStatus(),
            trip.getInviteCode(),
            DEFAULT_MAX_MEMBERS,
            getTripImageUrls(trip),
            getTripPhotoUrls(trip.getId()));

        TripRole myRole = TripRoleMapper.fromMemberRole(membership.getRole());
        return new TripDetailResponse(tripInfo, myRole.name(), members, highlights, recentActivities);
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
    public TripDetailResponse createTrip(Long currentUserId, CreateTripRequest request) {
        UserEntity leader = this.userJpaRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        TripEntity trip = TripEntity.builder()
                .name(request.name())
                .location(request.destination())
                .startDate(request.startDate())
                .endDate(request.endDate())
            .coverImageUrl(request.coverImageUrl())
            .placeId(request.placeId())
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

        this.tripActivityLogService.log(saved, leader, "CREATE_TRIP", "TRIP", saved.getId(), "trip created");
        return getTripDetail(saved.getId(), currentUserId);
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

    @Transactional(readOnly = true)
    public TripInfoResponse getTripByInviteCode(String code) {
        String inviteCode = normalizeRequired(code, "inviteCode").toUpperCase(Locale.ROOT);
        TripEntity trip = this.tripJpaRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new ResourceNotFoundException("Mã mời không hợp lệ hoặc không tìm thấy chuyến đi"));
        
        if (trip.getInviteCodeExpiredAt() != null && trip.getInviteCodeExpiredAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Mã mời đã hết hạn");
        }

        return new TripInfoResponse(
            trip.getId(),
            trip.getName(),
            trip.getLocation(),
            trip.getCoverImageUrl(),
            trip.getPlaceId(),
            trip.getDescription(),
            trip.getStartDate(),
            trip.getEndDate(),
            trip.getBudgetMin(),
            trip.getBudgetMax(),
            trip.getStatus(),
            trip.getInviteCode(),
            DEFAULT_MAX_MEMBERS,
            getTripImageUrls(trip),
            getTripPhotoUrls(trip.getId()));
    }

    @Transactional(readOnly = true)
    public List<TripPhotoResponse> getTripPhotos(Long tripId, Long currentUserId) {
        requireActiveMemberTrip(tripId, currentUserId);
        return this.tripPhotoJpaRepository.findByTripIdOrderByUploadedAtDescIdDesc(tripId)
                .stream()
                .map(this::toTripPhotoResponse)
                .toList();
    }

    @Transactional
    public List<TripPhotoResponse> addTripPhotos(Long tripId, Long currentUserId, List<String> imageUrls) {
        TripEntity trip = requireActiveMemberTrip(tripId, currentUserId);
        UserEntity uploader = findUser(currentUserId);
        List<String> sanitizedUrls = sanitizeImageUrls(imageUrls);
        if (sanitizedUrls.isEmpty()) {
            throw new IllegalArgumentException("imageUrls is required");
        }

        sanitizedUrls.stream()
                .map(imageUrl -> TripPhotoEntity.builder()
                        .trip(trip)
                        .uploadedBy(uploader)
                        .imageUrl(imageUrl)
                        .build())
                .forEach(this.tripPhotoJpaRepository::save);
        this.tripActivityLogService.log(trip, uploader, "ADD_TRIP_PHOTOS", "TRIP", tripId, "trip photos added");
        return getTripPhotos(tripId, currentUserId);
    }

    @Transactional
    public PostModel publishTripPost(Long tripId, Long currentUserId, CreateTripPostRequest request) {
        TripEntity trip = requireLeaderTrip(tripId, currentUserId);
        if (resolveDashboardStatus(trip) != TripStatus.COMPLETED) {
            throw new IllegalStateException("Trip must be completed before publishing a post");
        }

        List<String> photoUrls = getTripPhotoUrls(tripId);
        if (photoUrls.isEmpty()) {
            throw new IllegalStateException("Trip has no uploaded photos");
        }

        String description = request == null ? null : request.description();
        String normalizedDescription = description == null || description.isBlank()
                ? buildDefaultTripPostDescription(trip)
                : description.trim();

        PostModel post = PostModel.builder()
                .description(normalizedDescription)
                .imageUrls(photoUrls)
                .travelPlaceId(trip.getPlaceId())
                .userId(currentUserId)
                .build();
        PostModel savedPost = this.postRepository.save(currentUserId, post);
        this.userRepository.incrementPosts(currentUserId);
        this.tripActivityLogService.log(trip, trip.getLeader(), "PUBLISH_TRIP_POST", "POST", savedPost.getId(), "trip post published");
        return savedPost;
    }

    private List<String> getTripImageUrls(TripEntity trip) {
        if (trip.getPlaceId() == null) {
            return List.of();
        }

        return this.travelPlaceImageJpaRepository
            .findByPlaceIdOrderByMainDescIdAsc(trip.getPlaceId())
            .stream()
            .map(image -> image.getImageUrl())
            .filter(imageUrl -> imageUrl != null && !imageUrl.isBlank())
            .distinct()
            .toList();
    }

    private List<String> getTripPhotoUrls(Long tripId) {
        return this.tripPhotoJpaRepository.findByTripIdOrderByUploadedAtDescIdDesc(tripId)
                .stream()
                .map(TripPhotoEntity::getImageUrl)
                .filter(imageUrl -> imageUrl != null && !imageUrl.isBlank())
                .distinct()
                .toList();
    }

    private List<String> sanitizeImageUrls(List<String> imageUrls) {
        if (imageUrls == null) {
            return List.of();
        }
        return imageUrls.stream()
                .map(imageUrl -> imageUrl == null ? "" : imageUrl.trim())
                .filter(imageUrl -> !imageUrl.isBlank())
                .distinct()
                .limit(30)
                .toList();
    }

    private TripPhotoResponse toTripPhotoResponse(TripPhotoEntity photo) {
        UserEntity uploader = photo.getUploadedBy();
        return new TripPhotoResponse(
                photo.getId(),
                photo.getImageUrl(),
                uploader == null ? null : uploader.getId(),
                uploader == null ? null : displayName(uploader),
                photo.getUploadedAt());
    }

    private String buildDefaultTripPostDescription(TripEntity trip) {
        String dateRange = buildDateRange(trip.getStartDate(), trip.getEndDate());
        return "Nhìn lại chuyến đi " + trip.getName() + " tại " + trip.getLocation() + " (" + dateRange + ").";
    }

    @Transactional
    public JoinTripResultResponse joinByInviteCode(Long currentUserId, String rawInviteCode) {
        String inviteCode = normalizeRequired(rawInviteCode, "inviteCode").toUpperCase(Locale.ROOT);
        TripEntity trip = this.tripJpaRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new IllegalArgumentException("Mã mời không hợp lệ"));
        UserEntity currentUser = findUser(currentUserId);
        if (this.bankAccountJpaRepository.findFirstByUserIdAndIsDefaultTrue(currentUserId).isEmpty()) {
            throw new IllegalStateException("Bank account is required to join trip");
        }

        if (trip.getInviteCodeExpiredAt() != null && trip.getInviteCodeExpiredAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Mã mời đã hết hạn");
        }

        Optional<TripMemberEntity> existingMemberOpt = this.tripMemberJpaRepository.findByTripIdAndUserId(trip.getId(), currentUserId);
        if (existingMemberOpt.isPresent()) {
            TripMemberStatus status = existingMemberOpt.get().getStatus();
            if (status == TripMemberStatus.ACTIVE) {
                throw new DataIntegrityViolationException("Bạn đã là thành viên của chuyến đi này");
            }
            if (status == TripMemberStatus.PENDING) {
                throw new DataIntegrityViolationException("Yêu cầu tham gia đã được gửi");
            }
        }

        TripMemberEntity member = existingMemberOpt.orElseGet(() -> TripMemberEntity.builder()
                .trip(trip)
                .user(currentUser)
                .role(TripMemberRole.MEMBER)
                .status(TripMemberStatus.PENDING)
                .build());

        member.setStatus(TripMemberStatus.PENDING);
        member.setRole(TripMemberRole.MEMBER);
        member.setRequestedAt(java.time.Instant.now());
        member.setRespondedAt(null);
        TripMemberEntity savedMember = this.tripMemberJpaRepository.save(member);
        this.tripActivityLogService.log(trip, currentUser, "REQUEST_JOIN", "USER", currentUserId, "join request submitted");
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
            formatDate(trip.getStartDate()),
            formatDate(trip.getEndDate()),
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

    private TripActivityItemResponse toActivityItem(edu.uet.travel_hub.infrastructure.persistence.entity.TripActivityLogEntity log) {
        String type = mapActivityType(log.getActionType(), log.getTargetType());
        if (type == null) {
            return null;
        }
        String actorName = log.getActor() == null ? null : displayName(log.getActor());
        return new TripActivityItemResponse(type, log.getDescription(), actorName, log.getCreatedAt());
    }

    private String mapActivityType(String actionType, String targetType) {
        if (actionType == null) {
            return null;
        }
        return switch (actionType) {
            case "ADD_EXPENSE" -> "EXPENSE_CREATED";
            case "CREATE_POLL" -> "POLL_CREATED";
            case "APPROVE_MEMBER", "REQUEST_JOIN" -> "MEMBER_JOINED";
            case "UPDATE_TRIP" -> "TRIP_UPDATED";
            default -> null;
        };
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

    private TripStatus resolveDashboardStatus(TripEntity trip) {
        if (trip.getStatus() == TripStatus.COMPLETED) {
            return TripStatus.COMPLETED;
        }
        return TripStatus.fromDates(trip.getStartDate(), trip.getEndDate());
    }
}
