package edu.uet.travel_hub.application.usecases;

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
import edu.uet.travel_hub.application.port.in.ItineraryUseCase;
import edu.uet.travel_hub.application.port.out.ItineraryAiGateway;
import edu.uet.travel_hub.application.port.out.ItineraryAiGateway.ItineraryAiGatewayRequest;
import edu.uet.travel_hub.application.port.out.ItineraryRepository;
import edu.uet.travel_hub.application.port.out.TripLookupRepository;
import edu.uet.travel_hub.domain.enums.TripMemberStatus;
import edu.uet.travel_hub.domain.model.ItineraryDayModel;
import edu.uet.travel_hub.domain.model.ItineraryDetailRowModel;
import edu.uet.travel_hub.domain.model.ItineraryModel;
import edu.uet.travel_hub.domain.model.ItineraryStopModel;
import edu.uet.travel_hub.domain.model.ItinerarySummaryModel;

@Service
public class ItineraryService implements ItineraryUseCase {
    private final ItineraryRepository itineraryRepository;
    private final TripLookupRepository tripLookupRepository;
    private final ItineraryAiGateway itineraryAiGateway;
    private final Map<String, StoredAiProposal> pendingAiProposals = new ConcurrentHashMap<>();

    public ItineraryService(
            ItineraryRepository itineraryRepository,
            TripLookupRepository tripLookupRepository,
            ItineraryAiGateway itineraryAiGateway) {
        this.itineraryRepository = itineraryRepository;
        this.tripLookupRepository = tripLookupRepository;
        this.itineraryAiGateway = itineraryAiGateway;
    }

    @Transactional(readOnly = true)
    @Override
    public List<ItinerarySummaryResponse> getMyItineraries(Long currentUserId) {
        return this.itineraryRepository.findSummariesByOwnerId(currentUserId)
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    @Transactional
    @Override
    public ItineraryResponse getItinerary(Long itineraryId, Long currentUserId) {
        ItineraryModel itinerary = findOwnedItinerary(itineraryId, currentUserId);
        ItineraryModel savedItinerary = this.itineraryRepository.saveAndFlush(itinerary);
        return toResponse(savedItinerary.getId(), currentUserId);
    }

    @Transactional
    @Override
    public ItineraryResponse getItineraryByGroupName(String groupName, Long currentUserId) {
        String normalizedGroupName = normalizeGroupName(groupName);
        ItineraryModel itinerary = this.itineraryRepository
                .findByOwnerIdAndGroupNameIgnoreCase(currentUserId, normalizedGroupName)
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary not found"));
        ItineraryModel savedItinerary = this.itineraryRepository.saveAndFlush(itinerary);
        return toResponse(savedItinerary.getId(), currentUserId);
    }

    @Transactional
    @Override
    public ItineraryResponse createItinerary(Long currentUserId, CreateItineraryRequest request) {
        String normalizedGroupName = normalizeGroupName(request.groupName());
        String tripName = findTripNameForItinerary(currentUserId, request.tripId(), normalizedGroupName);
        if (tripName != null) {
            normalizedGroupName = normalizeGroupName(tripName);
        }
        ensureGroupNameAvailable(currentUserId, normalizedGroupName, null);

        ItineraryModel itinerary = ItineraryModel.builder()
                .groupName(normalizedGroupName)
                .ownerId(currentUserId)
                .version(1)
                .build();

        ItineraryModel savedItinerary = this.itineraryRepository.saveAndFlush(itinerary);
        return toResponse(savedItinerary.getId(), currentUserId);
    }

    @Transactional
    @Override
    public ItineraryResponse updateItinerary(Long itineraryId, Long currentUserId, UpdateItineraryRequest request) {
        ItineraryModel itinerary = findOwnedItinerary(itineraryId, currentUserId);
        String normalizedGroupName = normalizeGroupName(request.groupName());
        ensureGroupNameAvailable(currentUserId, normalizedGroupName, itineraryId);

        itinerary.setGroupName(normalizedGroupName);
        bumpVersion(itinerary);
        ItineraryModel savedItinerary = this.itineraryRepository.saveAndFlush(itinerary);
        return toResponse(savedItinerary.getId(), currentUserId);
    }

    @Transactional
    @Override
    public void deleteItinerary(Long itineraryId, Long currentUserId) {
        ItineraryModel itinerary = findOwnedItinerary(itineraryId, currentUserId);
        this.itineraryRepository.delete(itinerary);
    }

    @Transactional
    @Override
    public ItineraryResponse createDay(Long itineraryId, Long currentUserId, CreateItineraryDayRequest request) {
        ItineraryModel itinerary = findOwnedItinerary(itineraryId, currentUserId);

        ItineraryDayModel day = ItineraryDayModel.builder()
                .dayIndex(itinerary.getDays().size() + 1)
                .label(normalizeRequiredText(request.label(), "label"))
                .dateLabel(normalizeRequiredText(request.dateLabel(), "dateLabel"))
                .build();

        itinerary.getDays().add(day);
        renumberDays(itinerary);
        bumpVersion(itinerary);
        ItineraryModel savedItinerary = this.itineraryRepository.saveAndFlush(itinerary);
        return toResponse(savedItinerary.getId(), currentUserId);
    }

    @Transactional
    @Override
    public ItineraryResponse updateDay(Long itineraryId, Long dayId, Long currentUserId,
            UpdateItineraryDayRequest request) {
        ItineraryModel itinerary = findOwnedItinerary(itineraryId, currentUserId);
        ItineraryDayModel day = findDay(itinerary, dayId);

        day.setLabel(normalizeRequiredText(request.label(), "label"));
        day.setDateLabel(normalizeRequiredText(request.dateLabel(), "dateLabel"));
        bumpVersion(itinerary);
        ItineraryModel savedItinerary = this.itineraryRepository.saveAndFlush(itinerary);
        return toResponse(savedItinerary.getId(), currentUserId);
    }

    @Transactional
    @Override
    public ItineraryResponse deleteDay(Long itineraryId, Long dayId, Long currentUserId) {
        ItineraryModel itinerary = findOwnedItinerary(itineraryId, currentUserId);
        ItineraryDayModel day = findDay(itinerary, dayId);

        removeDayFromItinerary(itinerary, day);
        renumberDays(itinerary);
        bumpVersion(itinerary);
        ItineraryModel savedItinerary = this.itineraryRepository.saveAndFlush(itinerary);
        return toResponse(savedItinerary.getId(), currentUserId);
    }

    @Transactional
    @Override
    public ItineraryResponse createStop(Long itineraryId, Long currentUserId, CreateItineraryStopRequest request) {
        ItineraryModel itinerary = findOwnedItinerary(itineraryId, currentUserId);
        ItineraryDayModel day = resolveDayForStop(itinerary, request);

        ItineraryStopModel stop = ItineraryStopModel.builder()
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
        ItineraryModel savedItinerary = this.itineraryRepository.saveAndFlush(itinerary);
        return toResponse(savedItinerary.getId(), currentUserId);
    }

    @Transactional
    @Override
    public ItineraryResponse updateStop(Long itineraryId, Long stopId, Long currentUserId,
            UpdateItineraryStopRequest request) {
        ItineraryModel itinerary = findOwnedItinerary(itineraryId, currentUserId);
        StopLocation stopLocation = findStop(itinerary, stopId);
        ItineraryStopModel stop = stopLocation.stop();
        ItineraryDayModel targetDay = findDay(itinerary, request.dayId());
        ItineraryDayModel currentDay = stopLocation.day();

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
            insertStop(targetDay, stop, request.sortOrder());
        } else {
            insertStop(targetDay, stop, request.sortOrder());
        }

        bumpVersion(itinerary);
        ItineraryModel savedItinerary = this.itineraryRepository.saveAndFlush(itinerary);
        return toResponse(savedItinerary.getId(), currentUserId);
    }

    @Transactional
    @Override
    public ItineraryResponse deleteStop(Long itineraryId, Long stopId, Long currentUserId) {
        ItineraryModel itinerary = findOwnedItinerary(itineraryId, currentUserId);
        StopLocation stopLocation = findStop(itinerary, stopId);
        ItineraryStopModel stop = stopLocation.stop();
        ItineraryDayModel day = stopLocation.day();

        removeStopFromDay(day, stop);
        normalizeStopOrders(day);
        bumpVersion(itinerary);
        ItineraryModel savedItinerary = this.itineraryRepository.saveAndFlush(itinerary);
        return toResponse(savedItinerary.getId(), currentUserId);
    }

    @Transactional
    @Override
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
    @Override
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

        ItineraryModel itinerary = findOwnedItinerary(itineraryId, currentUserId);
        ItineraryAiProposalResponse proposal = storedProposal.proposal();
        if (request.baseVersion() != proposal.baseVersion() || itinerary.getVersion() != proposal.baseVersion()) {
            throw new IllegalArgumentException("AI proposal is stale. Regenerate before applying changes.");
        }

        Set<String> selectedChangeIds = Set.copyOf(request.selectedChangeIds());
        proposal.changes().stream()
                .filter(change -> selectedChangeIds.contains(change.changeId()))
                .forEach(change -> applyAiChange(itinerary, change));

        bumpVersion(itinerary);
        ItineraryModel savedItinerary = this.itineraryRepository.saveAndFlush(itinerary);
        this.pendingAiProposals.remove(proposalId);
        return toResponse(savedItinerary.getId(), currentUserId);
    }

    private ItineraryResponse toResponse(Long itineraryId, Long currentUserId) {
        return toResponse(this.itineraryRepository.findDetailRowsByIdAndOwnerId(itineraryId, currentUserId));
    }

    private String findTripNameForItinerary(Long currentUserId, Long tripId, String groupName) {
        if (tripId != null && tripId > 0) {
            return this.tripLookupRepository
                    .findActiveMemberTripNameById(tripId, currentUserId, TripMemberStatus.ACTIVE)
                    .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
        }
        return this.tripLookupRepository
                .findFirstActiveMemberTripNameByName(currentUserId, TripMemberStatus.ACTIVE, groupName)
                .orElse(null);
    }

    private ItineraryDayModel findDayByIndex(List<ItineraryDayModel> days, int dayIndex) {
        return days.stream()
                .filter(day -> day.getDayIndex() == dayIndex)
                .findFirst()
                .orElse(null);
    }

    private ItineraryDayModel findDayByLabelAndDate(ItineraryModel itinerary, String label, String dateLabel) {
        return itinerary.getDays().stream()
                .filter(day -> label.equalsIgnoreCase(day.getLabel())
                        && dateLabel.equalsIgnoreCase(day.getDateLabel()))
                .findFirst()
                .orElse(null);
    }

    private ItineraryModel findOwnedItinerary(Long itineraryId, Long currentUserId) {
        return this.itineraryRepository.findByIdAndOwnerId(itineraryId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary not found"));
    }

    private ItineraryDayModel findDay(ItineraryModel itinerary, Long dayId) {
        return itinerary.getDays().stream()
                .filter(day -> dayId.equals(day.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary day not found"));
    }

    private ItineraryDayModel resolveDayForStop(ItineraryModel itinerary, CreateItineraryStopRequest request) {
        if (request.dayId() != null) {
            return findDay(itinerary, request.dayId());
        }

        Integer dayIndex = request.dayIndex();
        if (dayIndex != null && dayIndex > 0) {
            ItineraryDayModel existingByIndex = findDayByIndex(itinerary.getDays(), dayIndex);
            if (existingByIndex != null) {
                return existingByIndex;
            }
        }

        String normalizedLabel = normalizeOptionalText(request.dayLabel());
        String normalizedDateLabel = normalizeOptionalText(request.dayDateLabel());
        if (!normalizedLabel.isEmpty() && !normalizedDateLabel.isEmpty()) {
            ItineraryDayModel existingByLabel = findDayByLabelAndDate(itinerary, normalizedLabel, normalizedDateLabel);
            if (existingByLabel != null) {
                return existingByLabel;
            }
        }

        if (normalizedLabel.isEmpty() || normalizedDateLabel.isEmpty()) {
            throw new IllegalArgumentException("dayLabel and dayDateLabel are required when dayId is not provided");
        }

        ItineraryDayModel day = ItineraryDayModel.builder()
                .dayIndex(dayIndex != null && dayIndex > 0 ? dayIndex : itinerary.getDays().size() + 1)
                .label(normalizeRequiredText(normalizedLabel, "dayLabel"))
                .dateLabel(normalizeRequiredText(normalizedDateLabel, "dayDateLabel"))
                .build();

        itinerary.getDays().add(day);
        renumberDays(itinerary);
        return day;
    }

    private StopLocation findStop(ItineraryModel itinerary, Long stopId) {
        return itinerary.getDays().stream()
                .flatMap(day -> day.getStops().stream()
                        .filter(stop -> stopId.equals(stop.getId()))
                        .map(stop -> new StopLocation(day, stop)))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary stop not found"));
    }

    private ItineraryDayModel findDayByIdOrIndex(ItineraryModel itinerary, Long dayId, Integer dayIndex) {
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

    private void applyAiChange(ItineraryModel itinerary, ItineraryAiChangeResponse change) {
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

    private void applyAddDay(ItineraryModel itinerary, ItineraryAiDayDraftResponse dayDraft) {
        if (dayDraft == null) {
            throw new IllegalArgumentException("dayAfter is required for ADD_DAY");
        }

        ItineraryDayModel day = ItineraryDayModel.builder()
                .dayIndex(dayDraft.dayIndex() > 0 ? dayDraft.dayIndex() : itinerary.getDays().size() + 1)
                .label(normalizeRequiredText(dayDraft.label(), "label"))
                .dateLabel(normalizeRequiredText(dayDraft.dateLabel(), "dateLabel"))
                .build();

        itinerary.getDays().add(day);
        if (dayDraft.stops() != null) {
            for (ItineraryAiStopDraftResponse stopDraft : dayDraft.stops()) {
                ItineraryStopModel stop = buildStopFromAiDraft(day, stopDraft);
                day.getStops().add(stop);
            }
            normalizeStopOrders(day);
        }
        renumberDays(itinerary);
    }

    private void applyUpdateDay(ItineraryModel itinerary, ItineraryAiChangeResponse change) {
        ItineraryAiDayDraftResponse dayDraft = change.dayAfter();
        if (dayDraft == null) {
            throw new IllegalArgumentException("dayAfter is required for UPDATE_DAY");
        }
        Long targetDayId = change.targetDayId() != null ? change.targetDayId()
                : dayDraft != null ? dayDraft.id() : null;
        Integer targetDayIndex = dayDraft != null ? dayDraft.dayIndex() : null;
        ItineraryDayModel day = findDayByIdOrIndex(itinerary, targetDayId, targetDayIndex);
        day.setLabel(normalizeRequiredText(dayDraft.label(), "label"));
        day.setDateLabel(normalizeRequiredText(dayDraft.dateLabel(), "dateLabel"));
    }

    private void applyDeleteDay(ItineraryModel itinerary, ItineraryAiChangeResponse change) {
        ItineraryAiDayDraftResponse dayDraft = change.dayBefore();
        Long targetDayId = change.targetDayId() != null ? change.targetDayId()
                : dayDraft != null ? dayDraft.id() : null;
        Integer targetDayIndex = dayDraft != null ? dayDraft.dayIndex() : null;
        ItineraryDayModel day = findDayByIdOrIndex(itinerary, targetDayId, targetDayIndex);
        removeDayFromItinerary(itinerary, day);
        renumberDays(itinerary);
    }

    private void applyAddEvent(ItineraryModel itinerary, ItineraryAiChangeResponse change) {
        ItineraryAiStopDraftResponse stopDraft = change.stopAfter();
        if (stopDraft == null) {
            throw new IllegalArgumentException("stopAfter is required for ADD_EVENT");
        }
        Long targetDayId = firstNonNull(change.toDayId(), stopDraft.dayId());
        Integer targetDayIndex = firstNonNull(change.toDayIndex(), stopDraft.dayIndex());
        ItineraryDayModel day = findDayByIdOrIndex(itinerary, targetDayId, targetDayIndex);
        ItineraryStopModel stop = buildStopFromAiDraft(day, stopDraft);
        Integer requestedSortOrder = change.insertAt() == null ? stopDraft.sortOrder() : change.insertAt() + 1;
        insertStop(day, stop, requestedSortOrder);
    }

    private void applyUpdateEvent(ItineraryModel itinerary, ItineraryAiChangeResponse change) {
        ItineraryAiStopDraftResponse stopDraft = change.stopAfter();
        Long targetStopId = firstNonNull(change.targetStopId(), stopDraft != null ? stopDraft.id() : null);
        if (targetStopId == null || stopDraft == null) {
            throw new IllegalArgumentException("targetStopId and stopAfter are required for UPDATE_EVENT");
        }

        StopLocation stopLocation = findStop(itinerary, targetStopId);
        ItineraryStopModel stop = stopLocation.stop();
        ItineraryDayModel currentDay = stopLocation.day();
        Long targetDayId = firstNonNull(change.toDayId(), stopDraft.dayId());
        Integer targetDayIndex = firstNonNull(change.toDayIndex(), stopDraft.dayIndex());
        Long resolvedTargetDayId = targetDayId != null || targetDayIndex == null
                ? firstNonNull(targetDayId, currentDay.getId())
                : null;
        ItineraryDayModel targetDay = findDayByIdOrIndex(
                itinerary,
                resolvedTargetDayId,
                targetDayIndex);

        updateStopFromAiDraft(stop, stopDraft);
        if (!currentDay.getId().equals(targetDay.getId())) {
            removeStopFromDay(currentDay, stop);
            normalizeStopOrders(currentDay);
            insertStop(targetDay, stop, stopDraft.sortOrder());
        } else {
            insertStop(targetDay, stop, stopDraft.sortOrder());
        }
    }

    private void applyDeleteEvent(ItineraryModel itinerary, ItineraryAiChangeResponse change) {
        Long targetStopId = firstNonNull(change.targetStopId(),
                change.stopBefore() != null ? change.stopBefore().id() : null);
        if (targetStopId == null) {
            throw new IllegalArgumentException("targetStopId is required for DELETE_EVENT");
        }
        StopLocation stopLocation = findStop(itinerary, targetStopId);
        ItineraryStopModel stop = stopLocation.stop();
        ItineraryDayModel day = stopLocation.day();
        removeStopFromDay(day, stop);
        normalizeStopOrders(day);
    }

    private void applyMoveEvent(ItineraryModel itinerary, ItineraryAiChangeResponse change) {
        Long targetStopId = firstNonNull(change.targetStopId(),
                change.stopBefore() != null ? change.stopBefore().id() : null);
        if (targetStopId == null) {
            throw new IllegalArgumentException("targetStopId is required for MOVE_EVENT");
        }
        StopLocation stopLocation = findStop(itinerary, targetStopId);
        ItineraryStopModel stop = stopLocation.stop();
        ItineraryDayModel currentDay = stopLocation.day();
        ItineraryDayModel targetDay = findDayByIdOrIndex(itinerary, change.toDayId(), change.toDayIndex());

        if (!currentDay.getId().equals(targetDay.getId())) {
            removeStopFromDay(currentDay, stop);
            normalizeStopOrders(currentDay);
        }
        insertStop(targetDay, stop, change.toIndex() == null ? null : change.toIndex() + 1);
    }

    private ItineraryStopModel buildStopFromAiDraft(ItineraryDayModel day, ItineraryAiStopDraftResponse stopDraft) {
        return ItineraryStopModel.builder()
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

    private void updateStopFromAiDraft(ItineraryStopModel stop, ItineraryAiStopDraftResponse stopDraft) {
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

    private void removeDayFromItinerary(ItineraryModel itinerary, ItineraryDayModel day) {
        itinerary.getDays().removeIf(existingDay -> existingDay == day
                || (day.getId() != null && day.getId().equals(existingDay.getId())));
    }

    private void removeStopFromDay(ItineraryDayModel day, ItineraryStopModel stop) {
        day.getStops().removeIf(existingStop -> existingStop == stop
                || (stop.getId() != null && stop.getId().equals(existingStop.getId())));
    }

    private void ensureGroupNameAvailable(Long ownerId, String groupName, Long itineraryId) {
        boolean exists = itineraryId == null
                ? this.itineraryRepository.existsByOwnerIdAndGroupNameIgnoreCase(ownerId, groupName)
                : this.itineraryRepository.existsByOwnerIdAndGroupNameIgnoreCaseAndIdNot(ownerId, groupName,
                        itineraryId);
        if (exists) {
            throw new IllegalArgumentException("An itinerary with this groupName already exists");
        }
    }

    private void renumberDays(ItineraryModel itinerary) {
        List<ItineraryDayModel> orderedDays = new ArrayList<>(itinerary.getDays());
        orderedDays.sort(Comparator.comparing(ItineraryDayModel::getDayIndex).thenComparing(ItineraryDayModel::getId,
                Comparator.nullsLast(Long::compareTo)));
        for (int index = 0; index < orderedDays.size(); index++) {
            orderedDays.get(index).setDayIndex(index + 1);
        }
    }

    private void normalizeStopOrders(ItineraryDayModel day) {
        List<ItineraryStopModel> orderedStops = new ArrayList<>(day.getStops());
        orderedStops.sort(Comparator.comparing(ItineraryStopModel::getSortOrder)
                .thenComparing(ItineraryStopModel::getId, Comparator.nullsLast(Long::compareTo)));
        for (int index = 0; index < orderedStops.size(); index++) {
            orderedStops.get(index).setSortOrder(index + 1);
        }
    }

    private void insertStop(ItineraryDayModel day, ItineraryStopModel stop, Integer requestedSortOrder) {
        removeStopFromDay(day, stop);
        int insertIndex = resolveInsertIndex(day, requestedSortOrder);
        day.getStops().add(insertIndex, stop);
        renumberStopsInCurrentOrder(day);
    }

    private int resolveInsertIndex(ItineraryDayModel day, Integer requestedSortOrder) {
        if (requestedSortOrder == null || requestedSortOrder <= 0) {
            return day.getStops().size();
        }
        return Math.min(requestedSortOrder - 1, day.getStops().size());
    }

    private void renumberStopsInCurrentOrder(ItineraryDayModel day) {
        for (int index = 0; index < day.getStops().size(); index++) {
            day.getStops().get(index).setSortOrder(index + 1);
        }
    }

    private void bumpVersion(ItineraryModel itinerary) {
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

    private ItinerarySummaryResponse toSummaryResponse(ItinerarySummaryModel itinerary) {
        return new ItinerarySummaryResponse(
                itinerary.id(),
                itinerary.groupName(),
                itinerary.version(),
                Math.toIntExact(itinerary.totalDays()),
                Math.toIntExact(itinerary.totalStops()),
                itinerary.updatedAt());
    }

    private ItineraryResponse toResponse(List<ItineraryDetailRowModel> rows) {
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("Itinerary not found");
        }

        ItineraryDetailRowModel firstRow = rows.get(0);
        Map<Long, DayAccumulator> dayById = new LinkedHashMap<>();

        for (ItineraryDetailRowModel row : rows) {
            if (row.dayId() == null) {
                continue;
            }

            DayAccumulator day = dayById.computeIfAbsent(row.dayId(), ignored -> new DayAccumulator(
                    row.dayId(),
                    row.dayIndex(),
                    row.dayLabel(),
                    row.dateLabel()));

            if (row.stopId() != null) {
                day.stops().add(new ItineraryStopResponse(
                        row.stopId(),
                        row.sortOrder(),
                        row.startTime(),
                        row.endTime(),
                        row.title(),
                        row.placeName(),
                        row.note(),
                        row.transportToNext(),
                        row.estimatedCost(),
                        row.colorHex(),
                        row.iconName()));
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
                firstRow.itineraryId(),
                firstRow.groupName(),
                firstRow.version(),
                firstRow.ownerId(),
                days,
                firstRow.createdAt(),
                firstRow.updatedAt());
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

    private record StopLocation(
            ItineraryDayModel day,
            ItineraryStopModel stop) {
    }

    private record StoredAiProposal(
            Long itineraryId,
            Long ownerId,
            ItineraryAiProposalResponse proposal) {
    }
}
