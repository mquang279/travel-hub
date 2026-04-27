package edu.uet.travel_hub.application.usecases;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.uet.travel_hub.application.dto.request.CreateItineraryDayRequest;
import edu.uet.travel_hub.application.dto.request.CreateItineraryRequest;
import edu.uet.travel_hub.application.dto.request.CreateItineraryStopRequest;
import edu.uet.travel_hub.application.dto.request.UpdateItineraryDayRequest;
import edu.uet.travel_hub.application.dto.request.UpdateItineraryRequest;
import edu.uet.travel_hub.application.dto.request.UpdateItineraryStopRequest;
import edu.uet.travel_hub.application.dto.response.ItineraryDayResponse;
import edu.uet.travel_hub.application.dto.response.ItineraryResponse;
import edu.uet.travel_hub.application.dto.response.ItineraryStopResponse;
import edu.uet.travel_hub.application.dto.response.ItinerarySummaryResponse;
import edu.uet.travel_hub.application.exception.ResourceNotFoundException;
import edu.uet.travel_hub.infrastructure.persistence.entity.ItineraryDayEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.ItineraryEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.ItineraryStopEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.UserEntity;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.ItineraryDayJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.ItineraryJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.ItineraryStopJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.UserJpaRepository;

@Service
public class ItineraryService {
    private final ItineraryJpaRepository itineraryJpaRepository;
    private final ItineraryDayJpaRepository itineraryDayJpaRepository;
    private final ItineraryStopJpaRepository itineraryStopJpaRepository;
    private final UserJpaRepository userJpaRepository;

    public ItineraryService(
            ItineraryJpaRepository itineraryJpaRepository,
            ItineraryDayJpaRepository itineraryDayJpaRepository,
            ItineraryStopJpaRepository itineraryStopJpaRepository,
            UserJpaRepository userJpaRepository) {
        this.itineraryJpaRepository = itineraryJpaRepository;
        this.itineraryDayJpaRepository = itineraryDayJpaRepository;
        this.itineraryStopJpaRepository = itineraryStopJpaRepository;
        this.userJpaRepository = userJpaRepository;
    }

    @Transactional(readOnly = true)
    public List<ItinerarySummaryResponse> getMyItineraries(Long currentUserId) {
        return this.itineraryJpaRepository.findByOwnerIdOrderByUpdatedAtDescIdDesc(currentUserId)
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ItineraryResponse getItinerary(Long itineraryId, Long currentUserId) {
        return toResponse(findOwnedItinerary(itineraryId, currentUserId));
    }

    @Transactional(readOnly = true)
    public ItineraryResponse getItineraryByGroupName(String groupName, Long currentUserId) {
        return toResponse(this.itineraryJpaRepository.findByOwnerIdAndGroupNameIgnoreCase(currentUserId, normalizeGroupName(groupName))
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary not found")));
    }

    @Transactional
    public ItineraryResponse createItinerary(Long currentUserId, CreateItineraryRequest request) {
        String normalizedGroupName = normalizeGroupName(request.groupName());
        ensureGroupNameAvailable(currentUserId, normalizedGroupName, null);

        UserEntity owner = this.userJpaRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ItineraryEntity itinerary = ItineraryEntity.builder()
                .groupName(normalizedGroupName)
                .owner(owner)
                .version(1)
                .build();

        return toResponse(this.itineraryJpaRepository.save(itinerary));
    }

    @Transactional
    public ItineraryResponse updateItinerary(Long itineraryId, Long currentUserId, UpdateItineraryRequest request) {
        ItineraryEntity itinerary = findOwnedItinerary(itineraryId, currentUserId);
        String normalizedGroupName = normalizeGroupName(request.groupName());
        ensureGroupNameAvailable(currentUserId, normalizedGroupName, itineraryId);

        itinerary.setGroupName(normalizedGroupName);
        bumpVersion(itinerary);
        return toResponse(this.itineraryJpaRepository.save(itinerary));
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
        return toResponse(this.itineraryJpaRepository.save(itinerary));
    }

    @Transactional
    public ItineraryResponse updateDay(Long itineraryId, Long dayId, Long currentUserId, UpdateItineraryDayRequest request) {
        ItineraryEntity itinerary = findOwnedItinerary(itineraryId, currentUserId);
        ItineraryDayEntity day = findDay(itinerary, dayId);

        day.setLabel(normalizeRequiredText(request.label(), "label"));
        day.setDateLabel(normalizeRequiredText(request.dateLabel(), "dateLabel"));
        bumpVersion(itinerary);
        return toResponse(this.itineraryJpaRepository.save(itinerary));
    }

    @Transactional
    public ItineraryResponse deleteDay(Long itineraryId, Long dayId, Long currentUserId) {
        ItineraryEntity itinerary = findOwnedItinerary(itineraryId, currentUserId);
        ItineraryDayEntity day = findDay(itinerary, dayId);

        itinerary.getDays().remove(day);
        renumberDays(itinerary);
        bumpVersion(itinerary);
        return toResponse(this.itineraryJpaRepository.save(itinerary));
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
                .highlighted(Boolean.TRUE.equals(request.highlighted()))
                .build();

        day.getStops().add(stop);
        normalizeStopOrders(day);
        bumpVersion(itinerary);
        return toResponse(this.itineraryJpaRepository.save(itinerary));
    }

    @Transactional
    public ItineraryResponse updateStop(Long itineraryId, Long stopId, Long currentUserId, UpdateItineraryStopRequest request) {
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
        stop.setHighlighted(Boolean.TRUE.equals(request.highlighted()));

        if (!currentDay.getId().equals(targetDay.getId())) {
            currentDay.getStops().remove(stop);
            normalizeStopOrders(currentDay);
            targetDay.getStops().add(stop);
            stop.setDay(targetDay);
        }

        stop.setSortOrder(resolveInsertIndex(targetDay, request.sortOrder()));
        normalizeStopOrders(targetDay);
        bumpVersion(itinerary);
        return toResponse(this.itineraryJpaRepository.save(itinerary));
    }

    @Transactional
    public ItineraryResponse deleteStop(Long itineraryId, Long stopId, Long currentUserId) {
        ItineraryEntity itinerary = findOwnedItinerary(itineraryId, currentUserId);
        ItineraryStopEntity stop = findStop(itinerary, stopId);
        ItineraryDayEntity day = stop.getDay();

        day.getStops().remove(stop);
        normalizeStopOrders(day);
        bumpVersion(itinerary);
        return toResponse(this.itineraryJpaRepository.save(itinerary));
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

    private void ensureGroupNameAvailable(Long ownerId, String groupName, Long itineraryId) {
        boolean exists = itineraryId == null
                ? this.itineraryJpaRepository.existsByOwnerIdAndGroupNameIgnoreCase(ownerId, groupName)
                : this.itineraryJpaRepository.existsByOwnerIdAndGroupNameIgnoreCaseAndIdNot(ownerId, groupName, itineraryId);
        if (exists) {
            throw new IllegalArgumentException("An itinerary with this groupName already exists");
        }
    }

    private void renumberDays(ItineraryEntity itinerary) {
        List<ItineraryDayEntity> orderedDays = new ArrayList<>(itinerary.getDays());
        orderedDays.sort(Comparator.comparing(ItineraryDayEntity::getDayIndex).thenComparing(ItineraryDayEntity::getId, Comparator.nullsLast(Long::compareTo)));
        for (int index = 0; index < orderedDays.size(); index++) {
            orderedDays.get(index).setDayIndex(index + 1);
        }
    }

    private void normalizeStopOrders(ItineraryDayEntity day) {
        List<ItineraryStopEntity> orderedStops = new ArrayList<>(day.getStops());
        orderedStops.sort(Comparator.comparing(ItineraryStopEntity::getSortOrder).thenComparing(ItineraryStopEntity::getId, Comparator.nullsLast(Long::compareTo)));
        for (int index = 0; index < orderedStops.size(); index++) {
            orderedStops.get(index).setSortOrder(index + 1);
        }
    }

    private int resolveInsertIndex(ItineraryDayEntity day, Integer requestedSortOrder) {
        if (requestedSortOrder == null || requestedSortOrder <= 0) {
            return day.getStops().size() + 1;
        }
        return Math.min(requestedSortOrder, day.getStops().size() + 1);
    }

    private void bumpVersion(ItineraryEntity itinerary) {
        itinerary.setVersion(itinerary.getVersion() + 1);
    }

    private String normalizeGroupName(String value) {
        return normalizeRequiredText(value, "groupName");
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

    private ItinerarySummaryResponse toSummaryResponse(ItineraryEntity itinerary) {
        int totalStops = itinerary.getDays().stream().mapToInt(day -> day.getStops().size()).sum();
        return new ItinerarySummaryResponse(
                itinerary.getId(),
                itinerary.getGroupName(),
                itinerary.getVersion(),
                itinerary.getDays().size(),
                totalStops,
                itinerary.getUpdatedAt());
    }

    private ItineraryResponse toResponse(ItineraryEntity itinerary) {
        List<ItineraryDayResponse> days = itinerary.getDays().stream()
                .sorted(Comparator.comparing(ItineraryDayEntity::getDayIndex).thenComparing(ItineraryDayEntity::getId))
                .map(day -> new ItineraryDayResponse(
                        day.getId(),
                        day.getDayIndex(),
                        day.getLabel(),
                        day.getDateLabel(),
                        day.getStops().stream()
                                .sorted(Comparator.comparing(ItineraryStopEntity::getSortOrder).thenComparing(ItineraryStopEntity::getId))
                                .map(stop -> new ItineraryStopResponse(
                                        stop.getId(),
                                        stop.getSortOrder(),
                                        stop.getStartTime(),
                                        stop.getEndTime(),
                                        stop.getTitle(),
                                        stop.getPlaceName(),
                                        stop.getNote(),
                                        stop.getTransportToNext(),
                                        stop.getEstimatedCost(),
                                        stop.isHighlighted()))
                                .toList()))
                .toList();

        return new ItineraryResponse(
                itinerary.getId(),
                itinerary.getGroupName(),
                itinerary.getVersion(),
                itinerary.getOwner().getId(),
                days,
                itinerary.getCreatedAt(),
                itinerary.getUpdatedAt());
    }
}
