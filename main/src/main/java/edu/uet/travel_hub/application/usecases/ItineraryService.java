package edu.uet.travel_hub.application.usecases;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.uet.travel_hub.application.dto.request.ApplyItineraryAiProposalRequest;
import edu.uet.travel_hub.application.dto.request.CreateItineraryAiProposalRequest;
import edu.uet.travel_hub.application.dto.request.CreateItineraryDayRequest;
import edu.uet.travel_hub.application.dto.request.CreateItineraryRequest;
import edu.uet.travel_hub.application.dto.request.CreateItineraryStopRequest;
import edu.uet.travel_hub.application.dto.request.UpdateItineraryDayRequest;
import edu.uet.travel_hub.application.dto.request.UpdateItineraryRequest;
import edu.uet.travel_hub.application.dto.request.UpdateItineraryStopRequest;
import edu.uet.travel_hub.application.dto.response.ItineraryAiChangeResponse;
import edu.uet.travel_hub.application.dto.response.ItineraryAiDayDraftResponse;
import edu.uet.travel_hub.application.dto.response.ItineraryAiProposalResponse;
import edu.uet.travel_hub.application.dto.response.ItineraryAiStopDraftResponse;
import edu.uet.travel_hub.application.dto.response.ItineraryDayResponse;
import edu.uet.travel_hub.application.dto.response.ItineraryResponse;
import edu.uet.travel_hub.application.dto.response.ItineraryStopResponse;
import edu.uet.travel_hub.application.dto.response.ItinerarySummaryResponse;
import edu.uet.travel_hub.application.exception.ResourceNotFoundException;
import edu.uet.travel_hub.application.port.out.ItineraryAiGateway;
import edu.uet.travel_hub.application.port.out.ItineraryAiGateway.ItineraryAiGatewayRequest;
import edu.uet.travel_hub.domain.enums.TripMemberStatus;
import edu.uet.travel_hub.infrastructure.persistence.entity.ItineraryDayEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.ItineraryEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.ItineraryStopEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.TripEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.UserEntity;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.ItineraryDetailRow;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.ItineraryJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.ItinerarySummaryProjection;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.TripJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.UserJpaRepository;

@Service
public class ItineraryService {
    private final ItineraryJpaRepository itineraryJpaRepository;
    private final TripJpaRepository tripJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final ItineraryAiGateway itineraryAiGateway;
    private final Map<String, StoredAiProposal> pendingAiProposals = new ConcurrentHashMap<>();

    public ItineraryService(
            ItineraryJpaRepository itineraryJpaRepository,
            TripJpaRepository tripJpaRepository,
            UserJpaRepository userJpaRepository,
            ItineraryAiGateway itineraryAiGateway) {
        this.itineraryJpaRepository = itineraryJpaRepository;
        this.tripJpaRepository = tripJpaRepository;
        this.userJpaRepository = userJpaRepository;
        this.itineraryAiGateway = itineraryAiGateway;
    }

    @Transactional(readOnly = true)
    public List<ItinerarySummaryResponse> getMyItineraries(Long currentUserId) {
        return this.itineraryJpaRepository.findSummariesByOwnerId(currentUserId)
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    @Transactional
    public ItineraryResponse getItinerary(Long itineraryId, Long currentUserId) {
        ItineraryEntity itinerary = findOwnedItinerary(itineraryId, currentUserId);
        // syncTripDateRangeDays(itinerary, findTripForItinerary(currentUserId, null,
        // itinerary.getGroupName()));
        ItineraryEntity savedItinerary = this.itineraryJpaRepository.saveAndFlush(itinerary);
        return toResponse(savedItinerary.getId(), currentUserId);
    }

    @Transactional
    public ItineraryResponse getItineraryByGroupName(String groupName, Long currentUserId) {
        String normalizedGroupName = normalizeGroupName(groupName);
        ItineraryEntity itinerary = this.itineraryJpaRepository
                .findByOwnerIdAndGroupNameIgnoreCase(currentUserId, normalizedGroupName)
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary not found"));
        // syncTripDateRangeDays(itinerary, findTripForItinerary(currentUserId, null,
        // normalizedGroupName));
        ItineraryEntity savedItinerary = this.itineraryJpaRepository.saveAndFlush(itinerary);
        return toResponse(savedItinerary.getId(), currentUserId);
    }

    @Transactional
    public ItineraryResponse createItinerary(Long currentUserId, CreateItineraryRequest request) {
        String normalizedGroupName = normalizeGroupName(request.groupName());
        TripEntity trip = findTripForItinerary(currentUserId, request.tripId(), normalizedGroupName);
        if (trip != null) {
            normalizedGroupName = normalizeGroupName(trip.getName());
        }
        ensureGroupNameAvailable(currentUserId, normalizedGroupName, null);

        UserEntity owner = this.userJpaRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ItineraryEntity itinerary = ItineraryEntity.builder()
                .groupName(normalizedGroupName)
                .owner(owner)
                .version(1)
                .build();

        // syncTripDateRangeDays(itinerary, trip);
        ItineraryEntity savedItinerary = this.itineraryJpaRepository.saveAndFlush(itinerary);
        return toResponse(savedItinerary.getId(), currentUserId);
    }

    @Transactional
    public ItineraryResponse updateItinerary(Long itineraryId, Long currentUserId, UpdateItineraryRequest request) {
        ItineraryEntity itinerary = findOwnedItinerary(itineraryId, currentUserId);
        String normalizedGroupName = normalizeGroupName(request.groupName());
        ensureGroupNameAvailable(currentUserId, normalizedGroupName, itineraryId);

        itinerary.setGroupName(normalizedGroupName);
        bumpVersion(itinerary);
        ItineraryEntity savedItinerary = this.itineraryJpaRepository.saveAndFlush(itinerary);
        return toResponse(savedItinerary.getId(), currentUserId);
    }

    @Transactional
    public void deleteItinerary(Long itineraryId, Long currentUserId) {
        ItineraryEntity itinerary = findOwnedItinerary(itineraryId, currentUserId);
        this.itineraryJpaRepository.delete(itinerary);
    }

    @Transactional
    public ItineraryResponse createDay(Long itineraryId, Long currentUserId, CreateItineraryDayRequest request) {
        ItineraryEntity itinerary = findOwnedItinerary(itineraryId, currentUserId);

        ItineraryDayEntity day = ItineraryDayEntity.builder()
                .itinerary(itinerary)
                .dayIndex(itinerary.getDays().size() + 1)
                .label(normalizeRequiredText(request.label(), "label"))
                .dateLabel(normalizeRequiredText(request.dateLabel(), "dateLabel"))
                .build();

        itinerary.getDays().add(day);
        renumberDays(itinerary);
        bumpVersion(itinerary);
        ItineraryEntity savedItinerary = this.itineraryJpaRepository.saveAndFlush(itinerary);
        return toResponse(savedItinerary.getId(), currentUserId);
    }

    @Transactional
    public ItineraryResponse updateDay(Long itineraryId, Long dayId, Long currentUserId,
            UpdateItineraryDayRequest request) {
        ItineraryEntity itinerary = findOwnedItinerary(itineraryId, currentUserId);
        ItineraryDayEntity day = findDay(itinerary, dayId);

        day.setLabel(normalizeRequiredText(request.label(), "label"));
        day.setDateLabel(normalizeRequiredText(request.dateLabel(), "dateLabel"));
        bumpVersion(itinerary);
        ItineraryEntity savedItinerary = this.itineraryJpaRepository.saveAndFlush(itinerary);
        return toResponse(savedItinerary.getId(), currentUserId);
    }

    @Transactional
    public ItineraryResponse deleteDay(Long itineraryId, Long dayId, Long currentUserId) {
        ItineraryEntity itinerary = findOwnedItinerary(itineraryId, currentUserId);
        ItineraryDayEntity day = findDay(itinerary, dayId);

        removeDayFromItinerary(itinerary, day);
        renumberDays(itinerary);
        bumpVersion(itinerary);
        ItineraryEntity savedItinerary = this.itineraryJpaRepository.saveAndFlush(itinerary);
        return toResponse(savedItinerary.getId(), currentUserId);
    }

    @Transactional
    public ItineraryResponse createStop(Long itineraryId, Long currentUserId, CreateItineraryStopRequest request) {
        ItineraryEntity itinerary = findOwnedItinerary(itineraryId, currentUserId);
        ItineraryDayEntity day = findDay(itinerary, request.dayId());

        ItineraryStopEntity stop = ItineraryStopEntity.builder()
                .day(day)
                .sortOrder(resolveInsertIndex(day, request.sortOrder()))
                .startTime(normalizeOptionalText(request.startTime()))
                .endTime(normalizeOptionalText(request.endTime()))
                .title(normalizeRequiredText(request.title(), "title"))
                .placeName(normalizeRequiredText(request.placeName(), "placeName"))
                .note(normalizeOptionalText(request.note()))
                .transportToNext(normalizeOptionalText(request.transportToNext()))
                .estimatedCost(normalizeOptionalText(request.estimatedCost()))
                .colorHex(request.colorHex())
                .iconName(normalizeOptionalText(request.iconName()))
                .build();

        insertStop(day, stop, request.sortOrder());
        bumpVersion(itinerary);
        ItineraryEntity savedItinerary = this.itineraryJpaRepository.saveAndFlush(itinerary);
        return toResponse(savedItinerary.getId(), currentUserId);
    }

    @Transactional
    public ItineraryResponse updateStop(Long itineraryId, Long stopId, Long currentUserId,
            UpdateItineraryStopRequest request) {
        ItineraryEntity itinerary = findOwnedItinerary(itineraryId, currentUserId);
        ItineraryStopEntity stop = findStop(itinerary, stopId);
        ItineraryDayEntity targetDay = findDay(itinerary, request.dayId());
        ItineraryDayEntity currentDay = stop.getDay();

        stop.setStartTime(normalizeOptionalText(request.startTime()));
        stop.setEndTime(normalizeOptionalText(request.endTime()));
        stop.setTitle(normalizeRequiredText(request.title(), "title"));
        stop.setPlaceName(normalizeRequiredText(request.placeName(), "placeName"));
        stop.setNote(normalizeOptionalText(request.note()));
        stop.setTransportToNext(normalizeOptionalText(request.transportToNext()));
        stop.setEstimatedCost(normalizeOptionalText(request.estimatedCost()));
        stop.setColorHex(request.colorHex());
        stop.setIconName(normalizeOptionalText(request.iconName()));

        if (!currentDay.getId().equals(targetDay.getId())) {
            removeStopFromDay(currentDay, stop);
            normalizeStopOrders(currentDay);
            stop.setDay(targetDay);
            insertStop(targetDay, stop, request.sortOrder());
        } else {
            insertStop(targetDay, stop, request.sortOrder());
        }

        bumpVersion(itinerary);
        ItineraryEntity savedItinerary = this.itineraryJpaRepository.saveAndFlush(itinerary);
        return toResponse(savedItinerary.getId(), currentUserId);
    }

    @Transactional
    public ItineraryResponse deleteStop(Long itineraryId, Long stopId, Long currentUserId) {
        ItineraryEntity itinerary = findOwnedItinerary(itineraryId, currentUserId);
        ItineraryStopEntity stop = findStop(itinerary, stopId);
        ItineraryDayEntity day = stop.getDay();

        removeStopFromDay(day, stop);
        normalizeStopOrders(day);
        bumpVersion(itinerary);
        ItineraryEntity savedItinerary = this.itineraryJpaRepository.saveAndFlush(itinerary);
        return toResponse(savedItinerary.getId(), currentUserId);
    }

    @Transactional
    public ItineraryAiProposalResponse createAiProposal(
            Long itineraryId,
            Long currentUserId,
            CreateItineraryAiProposalRequest request) {
        ItineraryResponse itinerary = getItinerary(itineraryId, currentUserId);
        if (request.baseVersion() != null && request.baseVersion() != itinerary.version()) {
            throw new IllegalArgumentException("Itinerary version has changed. Refresh before asking AI to edit.");
        }

        String task = normalizeAiTask(request.task(), itinerary.days().isEmpty());
        ItineraryAiProposalResponse proposal = this.itineraryAiGateway.createProposal(new ItineraryAiGatewayRequest(
                task,
                normalizeRequiredText(request.prompt(), "prompt"),
                normalizeInputType(request.inputType()),
                request.selectedDayId(),
                request.selectedDayIndex(),
                request.desiredDays(),
                normalizeOptionalText(request.destination()),
                itinerary));

        if (proposal == null || proposal.proposalId() == null || proposal.changes() == null) {
            throw new IllegalArgumentException("AI did not return a valid proposal");
        }

        this.pendingAiProposals.put(proposal.proposalId(), new StoredAiProposal(itineraryId, currentUserId, proposal));
        return proposal;
    }

    @Transactional
    public ItineraryResponse applyAiProposal(
            Long itineraryId,
            String proposalId,
            Long currentUserId,
            ApplyItineraryAiProposalRequest request) {
        StoredAiProposal storedProposal = this.pendingAiProposals.get(proposalId);
        if (storedProposal == null
                || !storedProposal.itineraryId().equals(itineraryId)
                || !storedProposal.ownerId().equals(currentUserId)) {
            throw new ResourceNotFoundException("AI proposal not found");
        }

        ItineraryEntity itinerary = findOwnedItinerary(itineraryId, currentUserId);
        ItineraryAiProposalResponse proposal = storedProposal.proposal();
        if (request.baseVersion() != proposal.baseVersion() || itinerary.getVersion() != proposal.baseVersion()) {
            throw new IllegalArgumentException("AI proposal is stale. Regenerate before applying changes.");
        }

        Set<String> selectedChangeIds = Set.copyOf(request.selectedChangeIds());
        proposal.changes().stream()
                .filter(change -> selectedChangeIds.contains(change.changeId()))
                .forEach(change -> applyAiChange(itinerary, change));

        bumpVersion(itinerary);
        ItineraryEntity savedItinerary = this.itineraryJpaRepository.saveAndFlush(itinerary);
        this.pendingAiProposals.remove(proposalId);
        return toResponse(savedItinerary.getId(), currentUserId);
    }

    private ItineraryResponse toResponse(Long itineraryId, Long currentUserId) {
        return toResponse(this.itineraryJpaRepository.findDetailRowsByIdAndOwnerId(itineraryId, currentUserId));
    }

    private TripEntity findTripForItinerary(Long currentUserId, Long tripId, String groupName) {
        if (tripId != null && tripId > 0) {
            return this.tripJpaRepository.findActiveMemberTripById(tripId, currentUserId, TripMemberStatus.ACTIVE)
                    .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
        }
        return this.tripJpaRepository.findActiveMemberTripsByName(currentUserId, TripMemberStatus.ACTIVE, groupName)
                .stream()
                .findFirst()
                .orElse(null);
    }

    // private void syncTripDateRangeDays(ItineraryEntity itinerary, TripEntity
    // trip) {
    // if (trip == null || trip.getStartDate() == null || trip.getEndDate() == null)
    // {
    // return;
    // }
    // LocalDate startDate = trip.getStartDate();
    // LocalDate endDate = trip.getEndDate();
    // if (endDate.isBefore(startDate)) {
    // return;
    // }

    // int dayCount = Math.toIntExact(ChronoUnit.DAYS.between(startDate, endDate) +
    // 1);
    // List<ItineraryDayEntity> orderedDays = new ArrayList<>(itinerary.getDays());
    // orderedDays.sort(Comparator.comparing(ItineraryDayEntity::getDayIndex).thenComparing(ItineraryDayEntity::getId,
    // Comparator.nullsLast(Long::compareTo)));

    // for (int dayIndex = 1; dayIndex <= dayCount; dayIndex++) {
    // ItineraryDayEntity day = findDayByIndex(orderedDays, dayIndex);
    // if (day == null) {
    // day = ItineraryDayEntity.builder()
    // .itinerary(itinerary)
    // .dayIndex(dayIndex)
    // .label(defaultDayLabel(dayIndex))
    // .dateLabel(defaultDateLabel(startDate.plusDays(dayIndex - 1L)))
    // .build();
    // itinerary.getDays().add(day);
    // orderedDays.add(day);
    // } else {
    // day.setLabel(defaultDayLabel(dayIndex));
    // day.setDateLabel(defaultDateLabel(startDate.plusDays(dayIndex - 1L)));
    // }
    // }

    // itinerary.getDays().removeIf(day -> day.getDayIndex() > dayCount &&
    // day.getStops().isEmpty());
    // renumberDays(itinerary);
    // }

    private ItineraryDayEntity findDayByIndex(List<ItineraryDayEntity> days, int dayIndex) {
        return days.stream()
                .filter(day -> day.getDayIndex() == dayIndex)
                .findFirst()
                .orElse(null);
    }

    private String defaultDayLabel(int dayIndex) {
        return "Day " + dayIndex;
    }

    private String defaultDateLabel(LocalDate date) {
        String dayOfWeek = switch (date.getDayOfWeek()) {
            case MONDAY -> "T2";
            case TUESDAY -> "T3";
            case WEDNESDAY -> "T4";
            case THURSDAY -> "T5";
            case FRIDAY -> "T6";
            case SATURDAY -> "T7";
            case SUNDAY -> "CN";
        };
        return "%s, %02d/%02d/%d".formatted(dayOfWeek, date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    private ItineraryEntity findOwnedItinerary(Long itineraryId, Long currentUserId) {
        return this.itineraryJpaRepository.findByIdAndOwnerId(itineraryId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary not found"));
    }

    private ItineraryDayEntity findDay(ItineraryEntity itinerary, Long dayId) {
        return itinerary.getDays().stream()
                .filter(day -> day.getId().equals(dayId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary day not found"));
    }

    private ItineraryStopEntity findStop(ItineraryEntity itinerary, Long stopId) {
        return itinerary.getDays().stream()
                .flatMap(day -> day.getStops().stream())
                .filter(stop -> stop.getId().equals(stopId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary stop not found"));
    }

    private ItineraryDayEntity findDayByIdOrIndex(ItineraryEntity itinerary, Long dayId, Integer dayIndex) {
        if (dayId != null) {
            return findDay(itinerary, dayId);
        }
        if (dayIndex != null) {
            return itinerary.getDays().stream()
                    .filter(day -> day.getDayIndex() == dayIndex)
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Itinerary day not found"));
        }
        throw new IllegalArgumentException("Target day is required");
    }

    private void applyAiChange(ItineraryEntity itinerary, ItineraryAiChangeResponse change) {
        switch (change.type()) {
            case "ADD_DAY" -> applyAddDay(itinerary, change.dayAfter());
            case "UPDATE_DAY" -> applyUpdateDay(itinerary, change);
            case "DELETE_DAY" -> applyDeleteDay(itinerary, change);
            case "ADD_EVENT" -> applyAddEvent(itinerary, change);
            case "UPDATE_EVENT" -> applyUpdateEvent(itinerary, change);
            case "DELETE_EVENT" -> applyDeleteEvent(itinerary, change);
            case "MOVE_EVENT" -> applyMoveEvent(itinerary, change);
            default -> throw new IllegalArgumentException("Unsupported AI change type: " + change.type());
        }
    }

    private void applyAddDay(ItineraryEntity itinerary, ItineraryAiDayDraftResponse dayDraft) {
        if (dayDraft == null) {
            throw new IllegalArgumentException("dayAfter is required for ADD_DAY");
        }

        ItineraryDayEntity day = ItineraryDayEntity.builder()
                .itinerary(itinerary)
                .dayIndex(dayDraft.dayIndex() > 0 ? dayDraft.dayIndex() : itinerary.getDays().size() + 1)
                .label(normalizeRequiredText(dayDraft.label(), "label"))
                .dateLabel(normalizeRequiredText(dayDraft.dateLabel(), "dateLabel"))
                .build();

        itinerary.getDays().add(day);
        if (dayDraft.stops() != null) {
            for (ItineraryAiStopDraftResponse stopDraft : dayDraft.stops()) {
                ItineraryStopEntity stop = buildStopFromAiDraft(day, stopDraft);
                day.getStops().add(stop);
            }
            normalizeStopOrders(day);
        }
        renumberDays(itinerary);
    }

    private void applyUpdateDay(ItineraryEntity itinerary, ItineraryAiChangeResponse change) {
        ItineraryAiDayDraftResponse dayDraft = change.dayAfter();
        if (dayDraft == null) {
            throw new IllegalArgumentException("dayAfter is required for UPDATE_DAY");
        }
        Long targetDayId = change.targetDayId() != null ? change.targetDayId()
                : dayDraft != null ? dayDraft.id() : null;
        Integer targetDayIndex = dayDraft != null ? dayDraft.dayIndex() : null;
        ItineraryDayEntity day = findDayByIdOrIndex(itinerary, targetDayId, targetDayIndex);
        day.setLabel(normalizeRequiredText(dayDraft.label(), "label"));
        day.setDateLabel(normalizeRequiredText(dayDraft.dateLabel(), "dateLabel"));
    }

    private void applyDeleteDay(ItineraryEntity itinerary, ItineraryAiChangeResponse change) {
        ItineraryAiDayDraftResponse dayDraft = change.dayBefore();
        Long targetDayId = change.targetDayId() != null ? change.targetDayId()
                : dayDraft != null ? dayDraft.id() : null;
        Integer targetDayIndex = dayDraft != null ? dayDraft.dayIndex() : null;
        ItineraryDayEntity day = findDayByIdOrIndex(itinerary, targetDayId, targetDayIndex);
        removeDayFromItinerary(itinerary, day);
        renumberDays(itinerary);
    }

    private void applyAddEvent(ItineraryEntity itinerary, ItineraryAiChangeResponse change) {
        ItineraryAiStopDraftResponse stopDraft = change.stopAfter();
        if (stopDraft == null) {
            throw new IllegalArgumentException("stopAfter is required for ADD_EVENT");
        }
        Long targetDayId = firstNonNull(change.toDayId(), stopDraft.dayId());
        Integer targetDayIndex = firstNonNull(change.toDayIndex(), stopDraft.dayIndex());
        ItineraryDayEntity day = findDayByIdOrIndex(itinerary, targetDayId, targetDayIndex);
        ItineraryStopEntity stop = buildStopFromAiDraft(day, stopDraft);
        Integer requestedSortOrder = change.insertAt() == null ? stopDraft.sortOrder() : change.insertAt() + 1;
        insertStop(day, stop, requestedSortOrder);
    }

    private void applyUpdateEvent(ItineraryEntity itinerary, ItineraryAiChangeResponse change) {
        ItineraryAiStopDraftResponse stopDraft = change.stopAfter();
        Long targetStopId = firstNonNull(change.targetStopId(), stopDraft != null ? stopDraft.id() : null);
        if (targetStopId == null || stopDraft == null) {
            throw new IllegalArgumentException("targetStopId and stopAfter are required for UPDATE_EVENT");
        }

        ItineraryStopEntity stop = findStop(itinerary, targetStopId);
        ItineraryDayEntity currentDay = stop.getDay();
        Long targetDayId = firstNonNull(change.toDayId(), stopDraft.dayId());
        Integer targetDayIndex = firstNonNull(change.toDayIndex(), stopDraft.dayIndex());
        Long resolvedTargetDayId = targetDayId != null || targetDayIndex == null
                ? firstNonNull(targetDayId, currentDay.getId())
                : null;
        ItineraryDayEntity targetDay = findDayByIdOrIndex(
                itinerary,
                resolvedTargetDayId,
                targetDayIndex);

        updateStopFromAiDraft(stop, stopDraft);
        if (!currentDay.getId().equals(targetDay.getId())) {
            removeStopFromDay(currentDay, stop);
            normalizeStopOrders(currentDay);
            stop.setDay(targetDay);
            insertStop(targetDay, stop, stopDraft.sortOrder());
        } else {
            insertStop(targetDay, stop, stopDraft.sortOrder());
        }
    }

    private void applyDeleteEvent(ItineraryEntity itinerary, ItineraryAiChangeResponse change) {
        Long targetStopId = firstNonNull(change.targetStopId(),
                change.stopBefore() != null ? change.stopBefore().id() : null);
        if (targetStopId == null) {
            throw new IllegalArgumentException("targetStopId is required for DELETE_EVENT");
        }
        ItineraryStopEntity stop = findStop(itinerary, targetStopId);
        ItineraryDayEntity day = stop.getDay();
        removeStopFromDay(day, stop);
        normalizeStopOrders(day);
    }

    private void applyMoveEvent(ItineraryEntity itinerary, ItineraryAiChangeResponse change) {
        Long targetStopId = firstNonNull(change.targetStopId(),
                change.stopBefore() != null ? change.stopBefore().id() : null);
        if (targetStopId == null) {
            throw new IllegalArgumentException("targetStopId is required for MOVE_EVENT");
        }
        ItineraryStopEntity stop = findStop(itinerary, targetStopId);
        ItineraryDayEntity currentDay = stop.getDay();
        ItineraryDayEntity targetDay = findDayByIdOrIndex(itinerary, change.toDayId(), change.toDayIndex());

        if (!currentDay.getId().equals(targetDay.getId())) {
            removeStopFromDay(currentDay, stop);
            normalizeStopOrders(currentDay);
            stop.setDay(targetDay);
        }
        insertStop(targetDay, stop, change.toIndex() == null ? null : change.toIndex() + 1);
    }

    private ItineraryStopEntity buildStopFromAiDraft(ItineraryDayEntity day, ItineraryAiStopDraftResponse stopDraft) {
        return ItineraryStopEntity.builder()
                .day(day)
                .sortOrder(stopDraft.sortOrder() == null ? day.getStops().size() + 1 : stopDraft.sortOrder())
                .startTime(normalizeOptionalText(stopDraft.startTime()))
                .endTime(normalizeOptionalText(stopDraft.endTime()))
                .title(normalizeRequiredText(stopDraft.title(), "title"))
                .placeName(normalizeRequiredText(stopDraft.placeName(), "placeName"))
                .note(normalizeOptionalText(stopDraft.note()))
                .transportToNext(normalizeOptionalText(stopDraft.transportToNext()))
                .estimatedCost(normalizeOptionalText(stopDraft.estimatedCost()))
                .colorHex(stopDraft.colorHex())
                .iconName(normalizeOptionalText(stopDraft.iconName()))
                .build();
    }

    private void updateStopFromAiDraft(ItineraryStopEntity stop, ItineraryAiStopDraftResponse stopDraft) {
        stop.setStartTime(normalizeOptionalText(stopDraft.startTime()));
        stop.setEndTime(normalizeOptionalText(stopDraft.endTime()));
        stop.setTitle(normalizeRequiredText(stopDraft.title(), "title"));
        stop.setPlaceName(normalizeRequiredText(stopDraft.placeName(), "placeName"));
        stop.setNote(normalizeOptionalText(stopDraft.note()));
        stop.setTransportToNext(normalizeOptionalText(stopDraft.transportToNext()));
        stop.setEstimatedCost(normalizeOptionalText(stopDraft.estimatedCost()));
        stop.setColorHex(stopDraft.colorHex());
        stop.setIconName(normalizeOptionalText(stopDraft.iconName()));
    }

    private void removeDayFromItinerary(ItineraryEntity itinerary, ItineraryDayEntity day) {
        itinerary.getDays().removeIf(existingDay -> existingDay.getId().equals(day.getId()));
    }

    private void removeStopFromDay(ItineraryDayEntity day, ItineraryStopEntity stop) {
        if (stop.getId() == null) {
            return;
        }
        day.getStops().removeIf(existingStop -> stop.getId().equals(existingStop.getId()));
    }

    private void ensureGroupNameAvailable(Long ownerId, String groupName, Long itineraryId) {
        boolean exists = itineraryId == null
                ? this.itineraryJpaRepository.existsByOwnerIdAndGroupNameIgnoreCase(ownerId, groupName)
                : this.itineraryJpaRepository.existsByOwnerIdAndGroupNameIgnoreCaseAndIdNot(ownerId, groupName,
                        itineraryId);
        if (exists) {
            throw new IllegalArgumentException("An itinerary with this groupName already exists");
        }
    }

    private void renumberDays(ItineraryEntity itinerary) {
        List<ItineraryDayEntity> orderedDays = new ArrayList<>(itinerary.getDays());
        orderedDays.sort(Comparator.comparing(ItineraryDayEntity::getDayIndex).thenComparing(ItineraryDayEntity::getId,
                Comparator.nullsLast(Long::compareTo)));
        for (int index = 0; index < orderedDays.size(); index++) {
            orderedDays.get(index).setDayIndex(index + 1);
        }
    }

    private void normalizeStopOrders(ItineraryDayEntity day) {
        List<ItineraryStopEntity> orderedStops = new ArrayList<>(day.getStops());
        orderedStops.sort(Comparator.comparing(ItineraryStopEntity::getSortOrder)
                .thenComparing(ItineraryStopEntity::getId, Comparator.nullsLast(Long::compareTo)));
        for (int index = 0; index < orderedStops.size(); index++) {
            orderedStops.get(index).setSortOrder(index + 1);
        }
    }

    private void insertStop(ItineraryDayEntity day, ItineraryStopEntity stop, Integer requestedSortOrder) {
        removeStopFromDay(day, stop);
        int insertIndex = resolveInsertIndex(day, requestedSortOrder);
        day.getStops().add(insertIndex, stop);
        renumberStopsInCurrentOrder(day);
    }

    private int resolveInsertIndex(ItineraryDayEntity day, Integer requestedSortOrder) {
        if (requestedSortOrder == null || requestedSortOrder <= 0) {
            return day.getStops().size();
        }
        return Math.min(requestedSortOrder - 1, day.getStops().size());
    }

    private void renumberStopsInCurrentOrder(ItineraryDayEntity day) {
        for (int index = 0; index < day.getStops().size(); index++) {
            day.getStops().get(index).setSortOrder(index + 1);
        }
    }

    private void bumpVersion(ItineraryEntity itinerary) {
        itinerary.setVersion(itinerary.getVersion() + 1);
    }

    private String normalizeGroupName(String value) {
        return normalizeRequiredText(value, "groupName");
    }

    private String normalizeAiTask(String value, boolean itineraryIsEmpty) {
        String normalized = value == null ? "" : value.trim().toUpperCase();
        if ("GENERATE_ITINERARY".equals(normalized) || "EDIT_ITINERARY".equals(normalized)) {
            return normalized;
        }
        return itineraryIsEmpty ? "GENERATE_ITINERARY" : "EDIT_ITINERARY";
    }

    private String normalizeInputType(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase();
        return "VOICE".equals(normalized) ? "VOICE" : "TEXT";
    }

    private String normalizeRequiredText(String value, String fieldName) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return normalized;
    }

    private String normalizeOptionalText(String value) {
        return value == null ? "" : value.trim();
    }

    private <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }

    private ItinerarySummaryResponse toSummaryResponse(ItinerarySummaryProjection itinerary) {
        return new ItinerarySummaryResponse(
                itinerary.getId(),
                itinerary.getGroupName(),
                itinerary.getVersion(),
                Math.toIntExact(itinerary.getTotalDays()),
                Math.toIntExact(itinerary.getTotalStops()),
                itinerary.getUpdatedAt());
    }

    private ItineraryResponse toResponse(List<ItineraryDetailRow> rows) {
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("Itinerary not found");
        }

        ItineraryDetailRow firstRow = rows.get(0);
        Map<Long, DayAccumulator> dayById = new LinkedHashMap<>();

        for (ItineraryDetailRow row : rows) {
            if (row.getDayId() == null) {
                continue;
            }

            DayAccumulator day = dayById.computeIfAbsent(row.getDayId(), ignored -> new DayAccumulator(
                    row.getDayId(),
                    row.getDayIndex(),
                    row.getDayLabel(),
                    row.getDateLabel()));

            if (row.getStopId() != null) {
                day.stops().add(new ItineraryStopResponse(
                        row.getStopId(),
                        row.getSortOrder(),
                        row.getStartTime(),
                        row.getEndTime(),
                        row.getTitle(),
                        row.getPlaceName(),
                        row.getNote(),
                        row.getTransportToNext(),
                        row.getEstimatedCost(),
                        row.getColorHex(),
                        row.getIconName()));
            }
        }

        List<ItineraryDayResponse> days = dayById.values().stream()
                .map(day -> new ItineraryDayResponse(
                        day.id(),
                        day.dayIndex(),
                        day.label(),
                        day.dateLabel(),
                        List.copyOf(day.stops())))
                .toList();

        return new ItineraryResponse(
                firstRow.getItineraryId(),
                firstRow.getGroupName(),
                firstRow.getVersion(),
                firstRow.getOwnerId(),
                days,
                firstRow.getCreatedAt(),
                firstRow.getUpdatedAt());
    }

    private record DayAccumulator(
            Long id,
            int dayIndex,
            String label,
            String dateLabel,
            List<ItineraryStopResponse> stops) {
        DayAccumulator(Long id, int dayIndex, String label, String dateLabel) {
            this(id, dayIndex, label, dateLabel, new ArrayList<>());
        }
    }

    private record StoredAiProposal(
            Long itineraryId,
            Long ownerId,
            ItineraryAiProposalResponse proposal) {
    }
}
