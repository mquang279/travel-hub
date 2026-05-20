package edu.uet.travel_hub.infrastructure.persistence.mapper;

import java.util.ArrayList;

import org.springframework.stereotype.Component;

import edu.uet.travel_hub.domain.model.ItineraryDayModel;
import edu.uet.travel_hub.domain.model.ItineraryDetailRowModel;
import edu.uet.travel_hub.domain.model.ItineraryModel;
import edu.uet.travel_hub.domain.model.ItineraryStopModel;
import edu.uet.travel_hub.domain.model.ItinerarySummaryModel;
import edu.uet.travel_hub.infrastructure.persistence.entity.ItineraryDayEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.ItineraryEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.ItineraryStopEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.UserEntity;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.ItineraryDetailRow;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.ItinerarySummaryProjection;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.UserJpaRepository;

@Component
public class ItineraryPersistenceMapper {
    private final UserJpaRepository userJpaRepository;

    public ItineraryPersistenceMapper(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    public ItineraryEntity toEntity(ItineraryModel model) {
        UserEntity owner = this.userJpaRepository.findById(model.getOwnerId())
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + model.getOwnerId()));

        ItineraryEntity entity = ItineraryEntity.builder()
                .id(model.getId())
                .groupName(model.getGroupName())
                .version(model.getVersion())
                .owner(owner)
                .createdAt(model.getCreatedAt())
                .updatedAt(model.getUpdatedAt())
                .days(new ArrayList<>())
                .build();

        if (model.getDays() != null) {
            for (ItineraryDayModel dayModel : model.getDays()) {
                ItineraryDayEntity dayEntity = toEntity(dayModel, entity);
                entity.getDays().add(dayEntity);
            }
        }
        return entity;
    }

    public ItineraryModel toDomain(ItineraryEntity entity) {
        ItineraryModel model = ItineraryModel.builder()
                .id(entity.getId())
                .groupName(entity.getGroupName())
                .version(entity.getVersion())
                .ownerId(entity.getOwner().getId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .days(new ArrayList<>())
                .build();

        if (entity.getDays() != null) {
            for (ItineraryDayEntity dayEntity : entity.getDays()) {
                model.getDays().add(toDomain(dayEntity));
            }
        }
        return model;
    }

    public ItinerarySummaryModel toSummaryDomain(ItinerarySummaryProjection projection) {
        return ItinerarySummaryModel.builder()
                .id(projection.getId())
                .groupName(projection.getGroupName())
                .version(projection.getVersion())
                .totalDays(projection.getTotalDays())
                .totalStops(projection.getTotalStops())
                .updatedAt(projection.getUpdatedAt())
                .build();
    }

    public ItineraryDetailRowModel toDetailRowDomain(ItineraryDetailRow row) {
        return ItineraryDetailRowModel.builder()
                .itineraryId(row.getItineraryId())
                .groupName(row.getGroupName())
                .version(row.getVersion())
                .ownerId(row.getOwnerId())
                .createdAt(row.getCreatedAt())
                .updatedAt(row.getUpdatedAt())
                .dayId(row.getDayId())
                .dayIndex(row.getDayIndex())
                .dayLabel(row.getDayLabel())
                .dateLabel(row.getDateLabel())
                .stopId(row.getStopId())
                .sortOrder(row.getSortOrder())
                .startTime(row.getStartTime())
                .endTime(row.getEndTime())
                .title(row.getTitle())
                .placeName(row.getPlaceName())
                .note(row.getNote())
                .transportToNext(row.getTransportToNext())
                .estimatedCost(row.getEstimatedCost())
                .colorHex(row.getColorHex())
                .iconName(row.getIconName())
                .build();
    }

    private ItineraryDayEntity toEntity(ItineraryDayModel model, ItineraryEntity itinerary) {
        ItineraryDayEntity entity = ItineraryDayEntity.builder()
                .id(model.getId())
                .itinerary(itinerary)
                .dayIndex(model.getDayIndex())
                .label(model.getLabel())
                .dateLabel(model.getDateLabel())
                .stops(new ArrayList<>())
                .build();

        if (model.getStops() != null) {
            for (ItineraryStopModel stopModel : model.getStops()) {
                entity.getStops().add(toEntity(stopModel, entity));
            }
        }
        return entity;
    }

    private ItineraryStopEntity toEntity(ItineraryStopModel model, ItineraryDayEntity day) {
        return ItineraryStopEntity.builder()
                .id(model.getId())
                .day(day)
                .sortOrder(model.getSortOrder())
                .startTime(model.getStartTime())
                .endTime(model.getEndTime())
                .title(model.getTitle())
                .placeName(model.getPlaceName())
                .note(model.getNote())
                .transportToNext(model.getTransportToNext())
                .estimatedCost(model.getEstimatedCost())
                .colorHex(model.getColorHex())
                .iconName(model.getIconName())
                .build();
    }

    private ItineraryDayModel toDomain(ItineraryDayEntity entity) {
        ItineraryDayModel model = ItineraryDayModel.builder()
                .id(entity.getId())
                .dayIndex(entity.getDayIndex())
                .label(entity.getLabel())
                .dateLabel(entity.getDateLabel())
                .stops(new ArrayList<>())
                .build();

        if (entity.getStops() != null) {
            for (ItineraryStopEntity stopEntity : entity.getStops()) {
                model.getStops().add(toDomain(stopEntity));
            }
        }
        return model;
    }

    private ItineraryStopModel toDomain(ItineraryStopEntity entity) {
        return ItineraryStopModel.builder()
                .id(entity.getId())
                .sortOrder(entity.getSortOrder())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .title(entity.getTitle())
                .placeName(entity.getPlaceName())
                .note(entity.getNote())
                .transportToNext(entity.getTransportToNext())
                .estimatedCost(entity.getEstimatedCost())
                .colorHex(entity.getColorHex())
                .iconName(entity.getIconName())
                .build();
    }
}
