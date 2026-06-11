package edu.uet.travel_hub.application.usecases;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.uet.travel_hub.application.dto.request.CreateTripActivityRequest;
import edu.uet.travel_hub.application.dto.request.UpdateTripActivityRequest;
import edu.uet.travel_hub.application.dto.response.TripActivityResponse;
import edu.uet.travel_hub.application.dto.response.TripDayResponse;
import edu.uet.travel_hub.application.exception.ResourceNotFoundException;
import edu.uet.travel_hub.application.port.in.TripActivityUseCase;
import edu.uet.travel_hub.infrastructure.persistence.entity.TripActivityEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.TripDayEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.TripEntity;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.TripActivityJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.TripDayJpaRepository;

@Service
public class TripActivityService implements TripActivityUseCase {
    private final TripService tripService;
    private final TripDayJpaRepository tripDayJpaRepository;
    private final TripActivityJpaRepository tripActivityJpaRepository;
    private final TripActivityLogService tripActivityLogService;

    public TripActivityService(
            TripService tripService,
            TripDayJpaRepository tripDayJpaRepository,
            TripActivityJpaRepository tripActivityJpaRepository,
            TripActivityLogService tripActivityLogService) {
        this.tripService = tripService;
        this.tripDayJpaRepository = tripDayJpaRepository;
        this.tripActivityJpaRepository = tripActivityJpaRepository;
        this.tripActivityLogService = tripActivityLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripDayResponse> listTripDays(Long tripId, Long currentUserId) {
        this.tripService.requireActiveMemberTrip(tripId, currentUserId);
        List<TripDayEntity> days = this.tripDayJpaRepository.findByTripIdOrderByDateAscIdAsc(tripId);
        Map<Long, List<TripActivityResponse>> activitiesByDayId = this.tripActivityJpaRepository
                .findByTripDayTripIdOrderByTripDayDateAscTripDayIdAscOrderIndexAscIdAsc(tripId)
                .stream()
                .collect(Collectors.groupingBy(
                        activity -> activity.getTripDay().getId(),
                        LinkedHashMap::new,
                        Collectors.mapping(this::toActivityResponse, Collectors.toList())));

        return days.stream()
                .map(day -> new TripDayResponse(
                        day.getId(),
                        day.getTrip().getId(),
                        day.getDate(),
                        day.getDayNumber(),
                        activitiesByDayId.getOrDefault(day.getId(), List.of())))
                .toList();
    }

    @Override
    @Transactional
    public TripActivityResponse createActivity(Long tripId, Long currentUserId, CreateTripActivityRequest request) {
        TripEntity trip = this.tripService.requireActiveMemberTrip(tripId, currentUserId);
        TripDayEntity tripDay = findOrCreateTripDay(trip, request.date());

        TripActivityEntity activity = TripActivityEntity.builder()
                .tripDay(tripDay)
                .title(normalizeRequired(request.title(), "title"))
                .description(normalizeOptional(request.description()))
                .startTime(request.startTime())
                .endTime(request.endTime())
                .locationName(normalizeOptional(request.locationName()))
                .address(normalizeOptional(request.address()))
                .type(normalizeOptional(request.type()))
                .orderIndex(resolveOrderIndex(request.orderIndex(), tripDay))
                .build();

        TripActivityEntity saved = this.tripActivityJpaRepository.save(activity);
        this.tripActivityLogService.log(
                trip,
                this.tripService.findUser(currentUserId),
                "ADD_ACTIVITY",
                "TRIP_ACTIVITY",
                saved.getId(),
                "activity added");
        return toActivityResponse(saved);
    }

    @Override
    @Transactional
    public TripActivityResponse updateActivity(
            Long tripId,
            Long activityId,
            Long currentUserId,
            UpdateTripActivityRequest request) {
        TripEntity trip = this.tripService.requireActiveMemberTrip(tripId, currentUserId);
        TripActivityEntity activity = this.tripActivityJpaRepository.findByIdAndTripDayTripId(activityId, tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip activity not found"));
        TripDayEntity previousDay = activity.getTripDay();
        TripDayEntity targetDay = findOrCreateTripDay(trip, request.date());

        activity.setTripDay(targetDay);
        activity.setTitle(normalizeRequired(request.title(), "title"));
        activity.setDescription(normalizeOptional(request.description()));
        activity.setStartTime(request.startTime());
        activity.setEndTime(request.endTime());
        activity.setLocationName(normalizeOptional(request.locationName()));
        activity.setAddress(normalizeOptional(request.address()));
        activity.setType(normalizeOptional(request.type()));
        activity.setOrderIndex(resolveOrderIndex(request.orderIndex(), targetDay));

        TripActivityEntity saved = this.tripActivityJpaRepository.save(activity);
        deleteEmptyDay(previousDay);
        this.tripActivityLogService.log(
                trip,
                this.tripService.findUser(currentUserId),
                "UPDATE_ACTIVITY",
                "TRIP_ACTIVITY",
                saved.getId(),
                "activity updated");
        return toActivityResponse(saved);
    }

    @Override
    @Transactional
    public void deleteActivity(Long tripId, Long activityId, Long currentUserId) {
        TripEntity trip = this.tripService.requireActiveMemberTrip(tripId, currentUserId);
        TripActivityEntity activity = this.tripActivityJpaRepository.findByIdAndTripDayTripId(activityId, tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip activity not found"));
        TripDayEntity tripDay = activity.getTripDay();

        this.tripActivityJpaRepository.delete(activity);
        this.tripActivityJpaRepository.flush();
        deleteEmptyDay(tripDay);
        this.tripActivityLogService.log(
                trip,
                this.tripService.findUser(currentUserId),
                "DELETE_ACTIVITY",
                "TRIP_ACTIVITY",
                activityId,
                "activity deleted");
    }

    private TripDayEntity findOrCreateTripDay(TripEntity trip, LocalDate date) {
        validateDateInTrip(trip, date);
        return this.tripDayJpaRepository.findByTripIdAndDate(trip.getId(), date)
                .orElseGet(() -> this.tripDayJpaRepository.save(TripDayEntity.builder()
                        .trip(trip)
                        .date(date)
                        .dayNumber(calculateDayNumber(trip, date))
                        .build()));
    }

    private void validateDateInTrip(TripEntity trip, LocalDate date) {
        if (trip.getStartDate() != null && date.isBefore(trip.getStartDate())) {
            throw new IllegalArgumentException("Activity date must not be before trip start date");
        }
        if (trip.getEndDate() != null && date.isAfter(trip.getEndDate())) {
            throw new IllegalArgumentException("Activity date must not be after trip end date");
        }
    }

    private int calculateDayNumber(TripEntity trip, LocalDate date) {
        if (trip.getStartDate() != null) {
            return Math.toIntExact(ChronoUnit.DAYS.between(trip.getStartDate(), date) + 1);
        }
        return this.tripDayJpaRepository.findByTripIdOrderByDateAscIdAsc(trip.getId()).size() + 1;
    }

    private int resolveOrderIndex(Integer requestedOrderIndex, TripDayEntity tripDay) {
        if (requestedOrderIndex != null && requestedOrderIndex > 0) {
            return requestedOrderIndex;
        }
        return tripDay.getActivities().stream()
                .map(TripActivityEntity::getOrderIndex)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }

    private void deleteEmptyDay(TripDayEntity tripDay) {
        if (tripDay == null || tripDay.getId() == null) {
            return;
        }
        if (this.tripActivityJpaRepository.countByTripDayId(tripDay.getId()) == 0) {
            this.tripDayJpaRepository.delete(tripDay);
        }
    }

    private TripActivityResponse toActivityResponse(TripActivityEntity activity) {
        return new TripActivityResponse(
                activity.getId(),
                activity.getTripDay().getId(),
                activity.getTitle(),
                activity.getDescription(),
                activity.getStartTime(),
                activity.getEndTime(),
                activity.getLocationName(),
                activity.getAddress(),
                activity.getType(),
                activity.getOrderIndex());
    }

    private String normalizeRequired(String value, String fieldName) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
